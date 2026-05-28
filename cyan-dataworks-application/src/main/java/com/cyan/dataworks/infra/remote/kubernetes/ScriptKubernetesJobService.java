package com.cyan.dataworks.infra.remote.kubernetes;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.infra.config.ScriptRuntimeProperties;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Shell和Python脚本Kubernetes Job运行服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class ScriptKubernetesJobService {

    /**
     * 脚本Runner，负责执行脚本并回调DataWorks
     */
    private static final String CALLBACK_RUNNER = """
            import json
            import hashlib
            import hmac
            import os
            import subprocess
            import sys
            import time
            import urllib.request
            from urllib.parse import quote, urlparse
            from datetime import datetime

            def now():
                return datetime.now().replace(microsecond=0).isoformat()

            def utc_now():
                return datetime.utcnow()

            def sign(key, msg):
                return hmac.new(key, msg.encode("utf-8"), hashlib.sha256).digest()

            def signing_key(secret_key, date_stamp, region, service):
                k_date = sign(("AWS4" + secret_key).encode("utf-8"), date_stamp)
                k_region = sign(k_date, region)
                k_service = sign(k_region, service)
                return sign(k_service, "aws4_request")

            def put_s3_object(endpoint, access_key, secret_key, bucket, object_key, body):
                parsed = urlparse(endpoint.rstrip("/"))
                host = parsed.netloc
                canonical_uri = "/" + bucket + "/" + quote(object_key, safe="/")
                url = endpoint.rstrip("/") + canonical_uri
                timestamp = utc_now()
                amz_date = timestamp.strftime("%Y%m%dT%H%M%SZ")
                date_stamp = timestamp.strftime("%Y%m%d")
                region = "us-east-1"
                service = "s3"
                payload_hash = hashlib.sha256(body).hexdigest()
                canonical_headers = f"host:{host}\\nx-amz-content-sha256:{payload_hash}\\nx-amz-date:{amz_date}\\n"
                signed_headers = "host;x-amz-content-sha256;x-amz-date"
                canonical_request = "\\n".join(["PUT", canonical_uri, "", canonical_headers, signed_headers, payload_hash])
                credential_scope = f"{date_stamp}/{region}/{service}/aws4_request"
                string_to_sign = "\\n".join(["AWS4-HMAC-SHA256", amz_date, credential_scope, hashlib.sha256(canonical_request.encode("utf-8")).hexdigest()])
                signature = hmac.new(signing_key(secret_key, date_stamp, region, service), string_to_sign.encode("utf-8"), hashlib.sha256).hexdigest()
                authorization = f"AWS4-HMAC-SHA256 Credential={access_key}/{credential_scope}, SignedHeaders={signed_headers}, Signature={signature}"
                request = urllib.request.Request(
                    url,
                    data=body,
                    headers={
                        "Authorization": authorization,
                        "x-amz-content-sha256": payload_hash,
                        "x-amz-date": amz_date,
                        "Content-Type": "text/plain; charset=utf-8",
                    },
                    method="PUT",
                )
                urllib.request.urlopen(request, timeout=30).read()

            script_type = os.environ.get("DATAWORKS_SCRIPT_TYPE", "shell")
            script_content = os.environ.get("DATAWORKS_SCRIPT_CONTENT", "")
            instance_id = os.environ.get("DATAWORKS_INSTANCE_ID", "")
            callback_url = os.environ.get("DATAWORKS_CALLBACK_URL", "")
            callback_token = os.environ.get("DATAWORKS_CALLBACK_TOKEN", "")
            started_at = now()
            start_ts = time.time()
            if script_type == "python":
                command = ["python", "-c", script_content]
            else:
                command = ["sh", "-c", script_content]
            proc = subprocess.run(command, capture_output=True, text=True)
            finished_at = now()
            output = (proc.stdout or "") + (proc.stderr or "")
            log_object_key = ""
            rustfs_endpoint = os.environ.get("RUSTFS_ENDPOINT", "")
            rustfs_access_key = os.environ.get("RUSTFS_ACCESS_KEY", "")
            rustfs_secret_key = os.environ.get("RUSTFS_SECRET_KEY", "")
            rustfs_log_bucket = os.environ.get("RUSTFS_LOG_BUCKET", "")
            rustfs_log_prefix = os.environ.get("RUSTFS_LOG_PREFIX", "script").strip("/")
            if rustfs_endpoint and rustfs_access_key and rustfs_secret_key and rustfs_log_bucket and instance_id:
                try:
                    log_object_key = f"{rustfs_log_prefix}/{instance_id}.log" if rustfs_log_prefix else f"{instance_id}.log"
                    put_s3_object(rustfs_endpoint, rustfs_access_key, rustfs_secret_key, rustfs_log_bucket, log_object_key, output.encode("utf-8"))
                except Exception as exc:
                    print(f"Failed to upload DataWorks script log to RustFS: {exc}", file=sys.stderr)
                    log_object_key = ""
            status = "SUCCESS" if proc.returncode == 0 else "FAILED"
            payload = {
                "status": status,
                "resultData": output if status == "SUCCESS" else "",
                "errorMessage": "" if status == "SUCCESS" else output,
                "logObjectKey": log_object_key,
                "exitCode": proc.returncode,
                "startedAt": started_at,
                "finishedAt": finished_at,
            }
            if callback_url:
                data = json.dumps(payload).encode("utf-8")
                request = urllib.request.Request(
                    callback_url,
                    data=data,
                    headers={
                        "Content-Type": "application/json",
                        "X-DataWorks-Callback-Token": callback_token,
                    },
                    method="POST",
                )
                urllib.request.urlopen(request, timeout=30).read()
            sys.exit(proc.returncode)
            """;

    /**
     * Kubernetes客户端
     */
    private final KubernetesClient kubernetesClient;

    /**
     * 脚本运行配置
     */
    private final ScriptRuntimeProperties properties;

    public ScriptKubernetesJobService(KubernetesClient kubernetesClient, ScriptRuntimeProperties properties) {
        this.kubernetesClient = kubernetesClient;
        this.properties = properties;
    }

    /**
     * 执行Shell脚本
     *
     * @param instanceId 实例ID
     * @param script     脚本内容
     * @param configJson 运行配置JSON
     * @return 执行日志
     */
    public String runShell(String instanceId, String script, String configJson) {
        ScriptRuntimeConfig runtimeConfig = parseRuntimeConfig(configJson, properties.getShellImage());
        return runScript(instanceId, "shell", runtimeConfig, List.of("sh", "-c", script));
    }

    /**
     * 异步提交Shell脚本
     *
     * @param instanceId 实例ID
     * @param script     脚本内容
     * @param configJson 运行配置JSON
     * @return Kubernetes Job名称
     */
    public String submitShell(String instanceId, String script, String configJson) {
        ScriptRuntimeConfig runtimeConfig = parseRuntimeConfig(configJson, properties.getShellImage());
        return submitScript(instanceId, "shell", runtimeConfig, script);
    }

    /**
     * 执行Python脚本
     *
     * @param instanceId 实例ID
     * @param script     脚本内容
     * @param configJson 运行配置JSON
     * @return 执行日志
     */
    public String runPython(String instanceId, String script, String configJson) {
        ScriptRuntimeConfig runtimeConfig = parseRuntimeConfig(configJson, properties.getPythonImage());
        return runScript(instanceId, "python", runtimeConfig, List.of("python", "-c", script));
    }

    /**
     * 异步提交Python脚本
     *
     * @param instanceId 实例ID
     * @param script     脚本内容
     * @param configJson 运行配置JSON
     * @return Kubernetes Job名称
     */
    public String submitPython(String instanceId, String script, String configJson) {
        ScriptRuntimeConfig runtimeConfig = parseRuntimeConfig(configJson, properties.getPythonImage());
        return submitScript(instanceId, "python", runtimeConfig, script);
    }

    /**
     * 异步提交脚本Kubernetes Job
     *
     * @param instanceId 实例ID
     * @param type       脚本类型
     * @param runtimeConfig 运行配置
     * @param script     脚本内容
     * @return Kubernetes Job名称
     */
    private String submitScript(String instanceId, String type, ScriptRuntimeConfig runtimeConfig, String script) {
        String namespace = Optional.ofNullable(properties.getNamespace()).filter(value -> !value.isBlank()).orElse("dataworks");
        String jobName = toK8sName("dataworks-" + type + "-" + instanceId);
        Map<String, String> labels = Map.of(
                "app", "cyan-dataworks",
                "dataworks-instance-id", instanceId,
                "dataworks-task-type", type
        );
        String callbackBaseUrl = Optional.ofNullable(properties.getCallbackBaseUrl())
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceAll("/+$", ""))
                .orElse("http://cyan-dataworks.pre.svc.cluster.local:8080");
        Job job = new JobBuilder()
                .withNewMetadata()
                .withName(jobName)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withBackoffLimit(0)
                .withActiveDeadlineSeconds((long) runtimeConfig.getTimeoutSeconds())
                .withTtlSecondsAfterFinished(Optional.ofNullable(properties.getTtlSecondsAfterFinished()).orElse(300))
                .withTemplate(new PodTemplateSpecBuilder()
                        .withNewMetadata()
                        .withLabels(labels)
                        .endMetadata()
                        .withSpec(new PodSpecBuilder()
                                .withRestartPolicy("Never")
                                .withContainers(new ContainerBuilder()
                                        .withName("main")
                                        .withImage(runtimeConfig.getImage())
                                        .withCommand("python", "-c", CALLBACK_RUNNER)
                                        .addNewEnv().withName("DATAWORKS_SCRIPT_TYPE").withValue(type).endEnv()
                                        .addNewEnv().withName("DATAWORKS_SCRIPT_CONTENT").withValue(script).endEnv()
                                        .addNewEnv().withName("DATAWORKS_INSTANCE_ID").withValue(instanceId).endEnv()
                                        .addNewEnv().withName("DATAWORKS_CALLBACK_URL").withValue(callbackBaseUrl + "/rpc/dataworks/job-instances/" + instanceId + "/callback").endEnv()
                                        .addNewEnv().withName("DATAWORKS_CALLBACK_TOKEN").withValue(Optional.ofNullable(properties.getCallbackToken()).orElse("")).endEnv()
                                        .addNewEnv().withName("RUSTFS_ENDPOINT").withValue(Optional.ofNullable(properties.getRustfsEndpoint()).orElse("")).endEnv()
                                        .addNewEnv().withName("RUSTFS_ACCESS_KEY").withValue(Optional.ofNullable(properties.getRustfsAccessKey()).orElse("")).endEnv()
                                        .addNewEnv().withName("RUSTFS_SECRET_KEY").withValue(Optional.ofNullable(properties.getRustfsSecretKey()).orElse("")).endEnv()
                                        .addNewEnv().withName("RUSTFS_LOG_BUCKET").withValue(Optional.ofNullable(properties.getLogBucket()).orElse("")).endEnv()
                                        .addNewEnv().withName("RUSTFS_LOG_PREFIX").withValue(Optional.ofNullable(properties.getLogBasePrefix()).orElse("script")).endEnv()
                                        .withResources(new ResourceRequirementsBuilder()
                                                .addToRequests("cpu", new Quantity(runtimeConfig.getCpu()))
                                                .addToRequests("memory", new Quantity(runtimeConfig.getMemory()))
                                                .addToLimits("cpu", new Quantity(runtimeConfig.getCpu()))
                                                .addToLimits("memory", new Quantity(runtimeConfig.getMemory()))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .endSpec()
                .build();
        log.info("准备异步创建脚本Kubernetes Job: instanceId={}, type={}, namespace={}, jobName={}, image={}, cpu={}, memory={}, timeoutSeconds={}, ttlSecondsAfterFinished={}",
                instanceId, type, namespace, jobName, runtimeConfig.getImage(), runtimeConfig.getCpu(), runtimeConfig.getMemory(),
                runtimeConfig.getTimeoutSeconds(), Optional.ofNullable(properties.getTtlSecondsAfterFinished()).orElse(300));
        kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        log.info("脚本Kubernetes Job异步创建完成: instanceId={}, type={}, namespace={}, jobName={}",
                instanceId, type, namespace, jobName);
        return jobName;
    }

    /**
     * 通过Kubernetes Job执行脚本
     *
     * @param instanceId 实例ID
     * @param type       脚本类型
     * @param runtimeConfig 运行配置
     * @param command    容器命令
     * @return 执行日志
     */
    private String runScript(String instanceId, String type, ScriptRuntimeConfig runtimeConfig, List<String> command) {
        String namespace = Optional.ofNullable(properties.getNamespace()).filter(value -> !value.isBlank()).orElse("dataworks");
        String jobName = toK8sName("dataworks-" + type + "-" + instanceId);
        Map<String, String> labels = Map.of(
                "app", "cyan-dataworks",
                "dataworks-instance-id", instanceId,
                "dataworks-task-type", type
        );
        Job job = new JobBuilder()
                .withNewMetadata()
                .withName(jobName)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withBackoffLimit(0)
                .withActiveDeadlineSeconds((long) runtimeConfig.getTimeoutSeconds())
                .withTtlSecondsAfterFinished(Optional.ofNullable(properties.getTtlSecondsAfterFinished()).orElse(300))
                .withTemplate(new PodTemplateSpecBuilder()
                        .withNewMetadata()
                        .withLabels(labels)
                        .endMetadata()
                        .withSpec(new PodSpecBuilder()
                                .withRestartPolicy("Never")
                                .withContainers(new ContainerBuilder()
                                        .withName("main")
                                        .withImage(runtimeConfig.getImage())
                                        .withCommand(command)
                                        .withResources(new ResourceRequirementsBuilder()
                                                .addToRequests("cpu", new Quantity(runtimeConfig.getCpu()))
                                                .addToRequests("memory", new Quantity(runtimeConfig.getMemory()))
                                                .addToLimits("cpu", new Quantity(runtimeConfig.getCpu()))
                                                .addToLimits("memory", new Quantity(runtimeConfig.getMemory()))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .endSpec()
                .build();
        log.info("准备创建脚本Kubernetes Job: instanceId={}, type={}, namespace={}, jobName={}, image={}, cpu={}, memory={}, timeoutSeconds={}, ttlSecondsAfterFinished={}",
                instanceId, type, namespace, jobName, runtimeConfig.getImage(), runtimeConfig.getCpu(), runtimeConfig.getMemory(),
                runtimeConfig.getTimeoutSeconds(), Optional.ofNullable(properties.getTtlSecondsAfterFinished()).orElse(300));
        kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        log.info("脚本Kubernetes Job创建完成: instanceId={}, type={}, namespace={}, jobName={}",
                instanceId, type, namespace, jobName);
        try {
            waitJobFinished(namespace, jobName, runtimeConfig.getTimeoutSeconds());
            String logs = readJobLogs(namespace, jobName);
            if (isJobFailed(namespace, jobName)) {
                log.warn("脚本Kubernetes Job执行失败: instanceId={}, type={}, namespace={}, jobName={}, logLength={}",
                        instanceId, type, namespace, jobName, logs == null ? 0 : logs.length());
                throw new SilentException(type + "任务执行失败：" + logs);
            }
            log.info("脚本Kubernetes Job执行成功: instanceId={}, type={}, namespace={}, jobName={}, logLength={}",
                    instanceId, type, namespace, jobName, logs == null ? 0 : logs.length());
            return logs;
        } finally {
            try {
                log.info("准备清理脚本Kubernetes Job: instanceId={}, type={}, namespace={}, jobName={}",
                        instanceId, type, namespace, jobName);
                kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).delete();
                log.info("脚本Kubernetes Job清理完成: instanceId={}, type={}, namespace={}, jobName={}",
                        instanceId, type, namespace, jobName);
            } catch (Exception e) {
                log.warn("清理脚本Kubernetes Job失败: instanceId={}, type={}, namespace={}, jobName={}, error={}",
                        instanceId, type, namespace, jobName, e.getMessage());
            }
        }
    }

    /**
     * 等待Job完成
     *
     * @param namespace 命名空间
     * @param jobName   Job名称
     * @param timeoutSeconds 超时时间秒数
     */
    private void waitJobFinished(String namespace, String jobName, int timeoutSeconds) {
        int intervalMs = Optional.ofNullable(properties.getPollIntervalMs()).orElse(1000);
        int configuredPollTimes = Optional.ofNullable(properties.getPollTimes()).orElse(120);
        int pollTimesByTimeout = Math.max(1, (int) Math.ceil(timeoutSeconds * 1000.0 / intervalMs));
        int pollTimes = Math.max(configuredPollTimes, pollTimesByTimeout);
        for (int i = 0; i < pollTimes; i++) {
            Job current = kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
            int succeeded = current == null || current.getStatus() == null || current.getStatus().getSucceeded() == null
                    ? 0
                    : current.getStatus().getSucceeded();
            int failed = current == null || current.getStatus() == null || current.getStatus().getFailed() == null
                    ? 0
                    : current.getStatus().getFailed();
            if (succeeded > 0) {
                log.info("脚本Kubernetes Job已成功完成: namespace={}, jobName={}, succeeded={}, failed={}, pollIndex={}",
                        namespace, jobName, succeeded, failed, i);
                return;
            }
            if (failed > 0) {
                log.warn("脚本Kubernetes Job已失败: namespace={}, jobName={}, succeeded={}, failed={}, pollIndex={}",
                        namespace, jobName, succeeded, failed, i);
                return;
            }
            sleep(intervalMs);
        }
        throw new SilentException("脚本任务执行超时：" + jobName);
    }

    /**
     * 解析脚本运行配置
     *
     * @param configJson 配置JSON
     * @param defaultImage 默认镜像
     * @return 脚本运行配置
     */
    private ScriptRuntimeConfig parseRuntimeConfig(String configJson, String defaultImage) {
        ScriptRuntimeConfig defaults = new ScriptRuntimeConfig()
                .setImage(defaultImage)
                .setCpu(Optional.ofNullable(properties.getCpu()).filter(value -> !value.isBlank()).orElse("0.5"))
                .setMemory(Optional.ofNullable(properties.getMemory()).filter(value -> !value.isBlank()).orElse("512Mi"))
                .setTimeoutSeconds(Optional.ofNullable(properties.getTimeoutSeconds()).orElse(300));
        if (configJson == null || configJson.isBlank()) {
            return defaults;
        }
        try {
            JSONObject root = JSON.parseObject(configJson);
            JSONObject script = root == null ? null : root.getJSONObject("script");
            if (script == null) {
                return defaults;
            }
            return new ScriptRuntimeConfig()
                    .setImage(Optional.ofNullable(script.getString("image")).filter(value -> !value.isBlank()).orElse(defaults.getImage()))
                    .setCpu(Optional.ofNullable(script.getString("cpu")).filter(value -> !value.isBlank()).orElse(defaults.getCpu()))
                    .setMemory(Optional.ofNullable(script.getString("memory")).filter(value -> !value.isBlank()).orElse(defaults.getMemory()))
                    .setTimeoutSeconds(Optional.ofNullable(script.getInteger("timeoutSeconds")).filter(value -> value >= 30).orElse(defaults.getTimeoutSeconds()));
        } catch (Exception e) {
            log.warn("解析脚本运行配置失败，将使用默认配置: {}", e.getMessage());
            return defaults;
        }
    }

    /**
     * 判断Job是否失败
     *
     * @param namespace 命名空间
     * @param jobName   Job名称
     * @return 是否失败
     */
    private boolean isJobFailed(String namespace, String jobName) {
        Job current = kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
        int failed = current == null || current.getStatus() == null || current.getStatus().getFailed() == null
                ? 0
                : current.getStatus().getFailed();
        return failed > 0;
    }

    /**
     * 读取Job Pod日志
     *
     * @param namespace 命名空间
     * @param jobName   Job名称
     * @return 日志内容
     */
    private String readJobLogs(String namespace, String jobName) {
        PodList podList = kubernetesClient.pods().inNamespace(namespace).withLabel("job-name", jobName).list();
        List<Pod> pods = Optional.ofNullable(podList.getItems()).orElse(List.of());
        if (pods.isEmpty()) {
            return "";
        }
        String podName = pods.get(0).getMetadata().getName();
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).inContainer("main").getLog();
    }

    /**
     * 当前线程休眠
     *
     * @param millis 毫秒数
     */
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SilentException("脚本任务执行被中断");
        }
    }

    /**
     * 转为Kubernetes名称
     *
     * @param name 原始名称
     * @return Kubernetes名称
     */
    private String toK8sName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
        return normalized.length() > 63 ? normalized.substring(0, 63).replaceAll("-$", "") : normalized;
    }

    /**
     * 脚本运行配置
     */
    private static class ScriptRuntimeConfig {

        /**
         * 运行镜像
         */
        private String image;

        /**
         * CPU限制
         */
        private String cpu;

        /**
         * 内存限制
         */
        private String memory;

        /**
         * 超时时间秒数
         */
        private int timeoutSeconds;

        /**
         * 获取运行镜像
         */
        public String getImage() {
            return image;
        }

        /**
         * 设置运行镜像
         */
        public ScriptRuntimeConfig setImage(String image) {
            this.image = image;
            return this;
        }

        /**
         * 获取CPU限制
         */
        public String getCpu() {
            return cpu;
        }

        /**
         * 设置CPU限制
         */
        public ScriptRuntimeConfig setCpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        /**
         * 获取内存限制
         */
        public String getMemory() {
            return memory;
        }

        /**
         * 设置内存限制
         */
        public ScriptRuntimeConfig setMemory(String memory) {
            this.memory = memory;
            return this;
        }

        /**
         * 获取超时时间秒数
         */
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        /**
         * 设置超时时间秒数
         */
        public ScriptRuntimeConfig setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }
    }
}
