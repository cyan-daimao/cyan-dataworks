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
        kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        try {
            waitJobFinished(namespace, jobName, runtimeConfig.getTimeoutSeconds());
            String logs = readJobLogs(namespace, jobName);
            if (isJobFailed(namespace, jobName)) {
                throw new SilentException(type + "任务执行失败：" + logs);
            }
            return logs;
        } finally {
            try {
                kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).delete();
            } catch (Exception e) {
                log.warn("清理脚本Kubernetes Job失败: {}", e.getMessage());
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
                return;
            }
            if (failed > 0) {
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
