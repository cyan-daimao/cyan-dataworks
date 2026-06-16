package com.cyan.dataworks.infra.remote.flink.operator;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.enums.JobLogRole;
import com.cyan.dataworks.infra.remote.flink.operator.bo.FlinkApplicationBO;
import com.cyan.dataworks.infra.remote.flink.operator.bo.FlinkPodLogBO;
import com.cyan.dataworks.infra.remote.flink.operator.cmd.FlinkApplicationSubmitCmd;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ContainerResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Flink Kubernetes Operator Application 模式提交服务
 * <p>
 * 通过 Fabric8 Kubernetes Client 管理 FlinkDeployment CR 和 ConfigMap。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class FlinkApplicationOperatorService {

    @Value("${flink.operator.namespace:dataworks}")
    private String defaultNamespace;

    @Value("${flink.operator.image:harbor.cyan.com/cyan/dataworks-flink-sql:2.0.1}")
    private String defaultImage;

    @Value("${flink.operator.jar-uri:local:///opt/flink/lib/flink-sql-runner.jar}")
    private String defaultJarUri;

    @Value("${flink.operator.entry-class:com.cyan.dataworks.flink.SqlRunner}")
    private String defaultEntryClass;

    @Value("${flink.operator.parallelism:1}")
    private Integer defaultParallelism;

    @Value("${rustfs.endpoint:http://10.0.0.2:9000}")
    private String rustfsEndpoint;

    @Value("${rustfs.accessKey:rustfsadmin}")
    private String rustfsAccessKey;

    @Value("${rustfs.secretKey:rustfsadmin}")
    private String rustfsSecretKey;

    private final KubernetesClient k8sClient;

    public FlinkApplicationOperatorService() {
        this.k8sClient = new KubernetesClientBuilder().build();
    }

    /**
     * 提交 Flink Application 模式作业
     *
     * @param cmd 提交命令
     * @return 提交结果
     */
    public FlinkApplicationBO submit(FlinkApplicationSubmitCmd cmd) {
        String deploymentName = cmd.getDeploymentName();
        String configMapName = cmd.getConfigMapName();
        String namespace = cmd.getNamespace() != null ? cmd.getNamespace() : defaultNamespace;
        String sql = cmd.getSql();

        try {
            createOrUpdateConfigMap(configMapName, sql, namespace);
            log.info("ConfigMap 创建/更新成功: {} in namespace {}", configMapName, namespace);

            String yaml = buildFlinkDeploymentYaml(cmd, namespace);
            GenericKubernetesResource flinkDeployment = Serialization.unmarshal(yaml, GenericKubernetesResource.class);
            k8sClient.resource(flinkDeployment).inNamespace(namespace).serverSideApply();
            log.info("FlinkDeployment 创建/更新成功: {} in namespace {}", deploymentName, namespace);

            List<Pod> pods = waitApplicationPods(deploymentName, namespace);
            return new FlinkApplicationBO()
                    .setDeploymentName(deploymentName)
                    .setConfigMapName(configMapName)
                    .setNamespace(namespace)
                    .setStatus("RUNNING")
                    .setRunning(true)
                    .setCompleted(false)
                    .setFailed(false)
                    .setMessage("Flink Application 提交成功")
                    .setJobManagerPodName(findJobManagerPodName(pods))
                    .setTaskManagerPodNames(findTaskManagerPodNames(pods));
        } catch (Exception e) {
            log.error("Flink Application 提交失败: {}", deploymentName, e);
            throw new SilentException("Flink Application 提交失败: " + e.getMessage());
        }
    }

    /**
     * 删除 Flink Application
     *
     * @param deploymentName Deployment 名称
     * @param configMapName  ConfigMap 名称
     */
    public void delete(String deploymentName, String configMapName) {
        String namespace = defaultNamespace;
        GenericKubernetesResource resource = get(deploymentName);
        if (resource != null) {
            try {
                k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                        .inNamespace(namespace)
                        .withName(deploymentName)
                        .delete();
                log.info("FlinkDeployment 删除成功: {}", deploymentName);
                waitFlinkDeploymentDeleted(deploymentName, namespace);
            } catch (SilentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("删除 FlinkDeployment 失败: {}, error: {}", deploymentName, e.getMessage());
            }
        }

        try {
            if (configMapName != null) {
                k8sClient.configMaps().inNamespace(namespace).withName(configMapName).delete();
                log.info("ConfigMap 删除成功: {}", configMapName);
            }
        } catch (Exception e) {
            log.warn("删除 ConfigMap 失败: {}, error: {}", configMapName, e.getMessage());
        }
    }

    /**
     * 等待FlinkDeployment真正删除，避免同名Application重建时被旧删除事件清理
     */
    private void waitFlinkDeploymentDeleted(String deploymentName, String namespace) {
        for (int i = 0; i < 90; i++) {
            GenericKubernetesResource resource = k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();
            if (resource == null) {
                return;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new SilentException("等待FlinkDeployment删除超时: " + deploymentName);
    }

    /**
     * 获取 FlinkDeployment
     *
     * @param deploymentName Deployment 名称
     * @return FlinkDeployment 资源
     */
    public GenericKubernetesResource get(String deploymentName) {
        try {
            return k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(defaultNamespace)
                    .withName(deploymentName)
                    .get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询FlinkDeployment状态
     *
     * @param deploymentName FlinkDeployment名称
     * @param namespace      命名空间
     * @return Flink Application状态
     */
    public FlinkApplicationBO getStatus(String deploymentName, String namespace) {
        String actualNamespace = namespace == null || namespace.isBlank() ? defaultNamespace : namespace;
        GenericKubernetesResource resource;
        try {
            resource = k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(actualNamespace)
                    .withName(deploymentName)
                    .get();
        } catch (Exception e) {
            log.warn("查询FlinkDeployment失败: namespace={}, deploymentName={}, error={}",
                    actualNamespace, deploymentName, e.getMessage());
            resource = null;
        }
        if (resource == null) {
            return new FlinkApplicationBO()
                    .setDeploymentName(deploymentName)
                    .setNamespace(actualNamespace)
                    .setStatus("NOT_FOUND")
                    .setRunning(false)
                    .setCompleted(false)
                    .setFailed(true)
                    .setMessage("FlinkDeployment不存在或已被删除");
        }
        Map<String, Object> additionalProperties = Optional.ofNullable(resource.getAdditionalProperties()).orElse(Map.of());
        Map<String, Object> status = asMap(additionalProperties.get("status"));
        Map<String, Object> jobStatus = asMap(status.get("jobStatus"));
        Map<String, Object> reconciliationStatus = asMap(status.get("reconciliationStatus"));
        String state = Optional.ofNullable(jobStatus.get("state"))
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(reconciliationStatus.get("state"))
                        .map(String::valueOf)
                        .orElse("UNKNOWN"));
        String message = Optional.ofNullable(jobStatus.get("error"))
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(status.get("error"))
                        .map(String::valueOf)
                        .orElse(""));
        return new FlinkApplicationBO()
                .setDeploymentName(deploymentName)
                .setConfigMapName(deploymentName + "-sql")
                .setNamespace(actualNamespace)
                .setStatus(state)
                .setRunning(isRunningState(state))
                .setCompleted(isCompletedState(state))
                .setFailed(isFailedState(state))
                .setMessage(message)
                .setJobManagerPodName(findJobManagerPodName(deploymentName, actualNamespace))
                .setTaskManagerPodNames(findTaskManagerPodNames(deploymentName, actualNamespace));
    }

    /**
     * 查询Flink Application Pod日志
     *
     * @param deploymentName FlinkDeployment名称
     * @param namespace      命名空间
     * @param role           日志角色
     * @param tailLines      尾部行数
     * @param previous       是否读取上一个已终止容器日志
     * @return Pod日志列表
     */
    public List<FlinkPodLogBO> getPodLogs(String deploymentName,
                                          String namespace,
                                          JobLogRole role,
                                          int tailLines,
                                          boolean previous) {
        String actualNamespace = namespace == null || namespace.isBlank() ? defaultNamespace : namespace;
        List<Pod> matchedPods = findApplicationPods(deploymentName, actualNamespace);

        return matchedPods.stream()
                .map(pod -> toPodLog(pod, actualNamespace, tailLines, previous))
                .filter(podLog -> role == null || role == JobLogRole.ALL || podLog.getRole() == role)
                .sorted(Comparator.comparing(FlinkPodLogBO::getRole).thenComparing(FlinkPodLogBO::getPodName))
                .toList();
    }

    /**
     * 查询JobManager Pod名称
     */
    public String findJobManagerPodName(String deploymentName, String namespace) {
        String actualNamespace = namespace == null || namespace.isBlank() ? defaultNamespace : namespace;
        return findJobManagerPodName(findApplicationPods(deploymentName, actualNamespace));
    }

    /**
     * 查询JobManager Pod名称
     */
    private String findJobManagerPodName(List<Pod> pods) {
        return pods.stream()
                .filter(pod -> resolvePodRole(pod) == JobLogRole.JOB_MANAGER)
                .findFirst()
                .map(this::resolvePodName)
                .orElse("");
    }

    /**
     * 查询TaskManager Pod名称列表
     */
    public List<String> findTaskManagerPodNames(String deploymentName, String namespace) {
        String actualNamespace = namespace == null || namespace.isBlank() ? defaultNamespace : namespace;
        return findTaskManagerPodNames(findApplicationPods(deploymentName, actualNamespace));
    }

    /**
     * 查询TaskManager Pod名称列表
     */
    private List<String> findTaskManagerPodNames(List<Pod> pods) {
        return pods.stream()
                .filter(pod -> resolvePodRole(pod) == JobLogRole.TASK_MANAGER)
                .map(this::resolvePodName)
                .toList();
    }

    /**
     * 等待Application关联Pod创建
     */
    private List<Pod> waitApplicationPods(String deploymentName, String namespace) {
        for (int i = 0; i < 10; i++) {
            List<Pod> pods = findApplicationPods(deploymentName, namespace);
            if (!pods.isEmpty()) {
                return pods;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
        return List.of();
    }

    /**
     * 查询Application关联Pod
     */
    private List<Pod> findApplicationPods(String deploymentName, String namespace) {
        List<Pod> pods = Optional.ofNullable(k8sClient.pods().inNamespace(namespace).list().getItems())
                .orElse(List.of());

        List<Pod> matchedPods = pods.stream()
                .filter(pod -> matchByLabels(pod, deploymentName))
                .toList();
        if (!matchedPods.isEmpty()) {
            return matchedPods;
        }
        return pods.stream()
                .filter(pod -> resolvePodName(pod).startsWith(deploymentName))
                .toList();
    }

    /**
     * 通过标签匹配FlinkDeployment关联Pod
     */
    private boolean matchByLabels(Pod pod, String deploymentName) {
        Map<String, String> labels = Optional.ofNullable(pod.getMetadata())
                .map(metadata -> metadata.getLabels())
                .orElse(Map.of());
        return labels.values().stream().anyMatch(value -> value != null && value.contains(deploymentName));
    }

    /**
     * 转换Pod日志
     */
    private FlinkPodLogBO toPodLog(Pod pod, String namespace, int tailLines, boolean previous) {
        String podName = resolvePodName(pod);
        JobLogRole podRole = resolvePodRole(pod);
        String containerName = resolveContainerName(pod);
        String logContent;
        try {
            ContainerResource container = k8sClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer(containerName);
            logContent = previous
                    ? container.terminated().tailingLines(tailLines).getLog()
                    : container.tailingLines(tailLines).getLog();
        } catch (Exception e) {
            log.warn("读取Flink Pod日志失败: pod={}, container={}, error={}", podName, containerName, e.getMessage());
            logContent = "读取Pod日志失败: " + e.getMessage();
        }
        return new FlinkPodLogBO()
                .setPodName(podName)
                .setRole(podRole)
                .setContainerName(containerName)
                .setLog(logContent == null ? "" : logContent);
    }

    /**
     * 解析Pod名称
     */
    private String resolvePodName(Pod pod) {
        return Optional.ofNullable(pod.getMetadata()).map(metadata -> metadata.getName()).orElse("");
    }

    /**
     * 识别Pod角色
     */
    private JobLogRole resolvePodRole(Pod pod) {
        String haystack = (Optional.ofNullable(pod.getMetadata()).map(metadata -> metadata.getName()).orElse("")
                + " "
                + Optional.ofNullable(pod.getMetadata()).map(metadata -> metadata.getLabels()).orElse(Map.of()))
                .toLowerCase(Locale.ROOT);
        if (haystack.contains("taskmanager")) {
            return JobLogRole.TASK_MANAGER;
        }
        return JobLogRole.JOB_MANAGER;
    }

    /**
     * 解析容器名称
     */
    private String resolveContainerName(Pod pod) {
        boolean hasMainContainer = Optional.ofNullable(pod.getSpec())
                .map(spec -> spec.getContainers())
                .orElse(List.of())
                .stream()
                .anyMatch(container -> "flink-main-container".equals(container.getName()));
        if (hasMainContainer) {
            return "flink-main-container";
        }
        return Optional.ofNullable(pod.getSpec())
                .map(spec -> spec.getContainers())
                .orElse(List.of())
                .stream()
                .findFirst()
                .map(container -> container.getName())
                .orElse("flink-main-container");
    }

    private void createOrUpdateConfigMap(String name, String sql, String namespace) {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .addToData("job.sql", sql)
                .build();
        k8sClient.resource(configMap).inNamespace(namespace).serverSideApply();
    }

    /**
     * 转换Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    /**
     * 是否运行中状态
     */
    private boolean isRunningState(String state) {
        String normalized = Optional.ofNullable(state).orElse("").toUpperCase(Locale.ROOT);
        return normalized.isBlank()
                || "UNKNOWN".equals(normalized)
                || "CREATED".equals(normalized)
                || "RECONCILING".equals(normalized)
                || "DEPLOYING".equals(normalized)
                || "RUNNING".equals(normalized);
    }

    /**
     * 是否成功完成状态
     */
    private boolean isCompletedState(String state) {
        String normalized = Optional.ofNullable(state).orElse("").toUpperCase(Locale.ROOT);
        return "FINISHED".equals(normalized)
                || "COMPLETED".equals(normalized);
    }

    /**
     * 是否失败状态
     */
    private boolean isFailedState(String state) {
        String normalized = Optional.ofNullable(state).orElse("").toUpperCase(Locale.ROOT);
        return "FAILED".equals(normalized)
                || "FAILING".equals(normalized)
                || "CANCELED".equals(normalized)
                || "CANCELLED".equals(normalized)
                || "SUSPENDED".equals(normalized)
                || "NOT_FOUND".equals(normalized);
    }

    private String buildFlinkDeploymentYaml(FlinkApplicationSubmitCmd cmd, String namespace) {
        String deploymentName = cmd.getDeploymentName();
        String configMapName = cmd.getConfigMapName();
        String image = cmd.getImage() != null ? cmd.getImage() : defaultImage;
        String jarUri = cmd.getJarUri() != null ? cmd.getJarUri() : defaultJarUri;
        String entryClass = cmd.getEntryClass() != null ? cmd.getEntryClass() : defaultEntryClass;
        int parallelism = cmd.getParallelism() != null ? cmd.getParallelism() : defaultParallelism;
        int taskManagerMemoryGb = cmd.getTaskManagerMemoryGb() != null ? cmd.getTaskManagerMemoryGb() : 1;
        double taskManagerCpu = cmd.getTaskManagerCpu() != null ? cmd.getTaskManagerCpu() : 0.5D;
        int jobManagerMemoryGb = cmd.getJobManagerMemoryGb() != null ? cmd.getJobManagerMemoryGb() : 1;
        double jobManagerCpu = cmd.getJobManagerCpu() != null ? cmd.getJobManagerCpu() : 0.5D;
        String flinkVersion = orDefault(cmd.getFlinkVersion(), "v2_0");
        String upgradeMode = orDefault(cmd.getUpgradeMode(), "last-state");
        String state = orDefault(cmd.getState(), "running");

        String flinkConfigurationYaml = renderFlinkConfiguration(cmd);
        String logConfigurationYaml = renderLogConfiguration();

        return String.format("""
                apiVersion: flink.apache.org/v1beta1
                kind: FlinkDeployment
                metadata:
                  name: %s
                  namespace: %s
                spec:
                  serviceAccount: flink
                  image: %s
                  flinkVersion: %s
                  logConfiguration:
                %s
                  jobManager:
                    resource:
                      memory: "%dg"
                      cpu: %s
                  taskManager:
                    resource:
                      memory: "%dg"
                      cpu: %s
                  flinkConfiguration:
                %s
                  job:
                    jarURI: %s
                    entryClass: %s
                    args:
                      - "/opt/flink/sql/job.sql"
                    parallelism: %d
                    upgradeMode: %s
                    state: %s
                  podTemplate:
                    spec:
                      imagePullSecrets:
                        - name: harbor-secret
                      containers:
                        - name: flink-main-container
                          imagePullPolicy: Always
                          volumeMounts:
                            - name: sql-volume
                              mountPath: /opt/flink/sql
                      volumes:
                        - name: sql-volume
                          configMap:
                            name: %s
                """,
                deploymentName, namespace, image, flinkVersion,
                logConfigurationYaml,
                jobManagerMemoryGb, formatCpu(jobManagerCpu),
                taskManagerMemoryGb, formatCpu(taskManagerCpu),
                flinkConfigurationYaml,
                jarUri, entryClass, parallelism,
                upgradeMode, state,
                configMapName);
    }

    /**
     * 渲染 flinkConfiguration 段：合并默认值与用户自定义，统一按 4 空格缩进输出
     */
    private String renderFlinkConfiguration(FlinkApplicationSubmitCmd cmd) {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.put("state.backend.type", orDefault(cmd.getStateBackendType(), "rocksdb"));
        merged.put("classloader.parent-first-patterns.additional", "com.codahale.metrics");
        merged.put("state.checkpoints.dir", "s3://flink/checkpoints/cyan-dataworks");
        merged.put("state.savepoints.dir", "s3://flink/savepoints/cyan-dataworks");
        merged.put("execution.checkpointing.interval", orDefault(cmd.getCheckpointInterval(), "60s"));
        merged.put("execution.checkpointing.timeout", orDefault(cmd.getCheckpointTimeout(), "600s"));
        merged.put("execution.checkpointing.max-concurrent-checkpoints",
                String.valueOf(cmd.getCheckpointMaxConcurrent() != null ? cmd.getCheckpointMaxConcurrent() : 1));
        merged.put("execution.checkpointing.min-pause", orDefault(cmd.getCheckpointMinPause(), "500ms"));
        merged.put("execution.checkpointing.mode", orDefault(cmd.getCheckpointMode(), "EXACTLY_ONCE"));
        merged.put("s3.endpoint", rustfsEndpoint);
        merged.put("s3.access-key", rustfsAccessKey);
        merged.put("s3.secret-key", rustfsSecretKey);
        merged.put("s3.path.style.access", "true");

        // 用户自定义键值对覆盖默认值
        Map<String, String> extra = cmd.getExtraFlinkConfiguration();
        if (extra != null) {
            extra.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    merged.put(key, value);
                }
            });
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(": ").append(escapeYamlScalar(entry.getValue())).append("\n");
        }
        // 去掉末尾换行交给上层 String.format 控制布局
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 渲染Flink日志配置，确保JobManager/TaskManager日志输出到容器stdout。
     */
    private String renderLogConfiguration() {
        Map<String, String> logConfiguration = new LinkedHashMap<>();
        logConfiguration.put("log4j-console.properties", """
                monitorInterval = 30
                rootLogger.level = INFO
                rootLogger.appenderRef.console.ref = ConsoleAppender

                appender.console.name = ConsoleAppender
                appender.console.type = CONSOLE
                appender.console.layout.type = PatternLayout
                appender.console.layout.pattern = %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p %-60c %x - %m%n

                logger.akka.name = akka
                logger.akka.level = INFO
                logger.kafka.name = org.apache.kafka
                logger.kafka.level = INFO
                logger.hadoop.name = org.apache.hadoop
                logger.hadoop.level = INFO
                logger.zookeeper.name = org.apache.zookeeper
                logger.zookeeper.level = INFO
                logger.netty.name = org.apache.flink.shaded.akka.org.jboss.netty.channel.DefaultChannelPipeline
                logger.netty.level = OFF
                """);
        logConfiguration.put("logback-console.xml", """
                <configuration>
                  <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
                    <encoder>
                      <pattern>%d{yyyy-MM-dd HH:mm:ss,SSS} %-5level %-60logger %X - %msg%n</pattern>
                    </encoder>
                  </appender>
                  <root level="INFO">
                    <appender-ref ref="console"/>
                  </root>
                </configuration>
                """);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : logConfiguration.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(": |").append("\n");
            for (String line : entry.getValue().stripTrailing().split("\\R", -1)) {
                sb.append("      ").append(line).append("\n");
            }
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 字符串值兜底
     */
    private String orDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * YAML 标量转义：含特殊字符时加双引号并转义内部反斜杠/引号
     */
    private String escapeYamlScalar(String raw) {
        if (raw == null) {
            return "\"\"";
        }
        if (raw.isEmpty()) {
            return "\"\"";
        }
        boolean needQuote = raw.chars().anyMatch(ch -> ch == ':' || ch == '#' || ch == '\n' || ch == '\r'
                || ch == '\'' || ch == '"' || ch == '{' || ch == '}' || ch == '[' || ch == ']'
                || ch == ',' || ch == '&' || ch == '*' || ch == '!' || ch == '|' || ch == '>'
                || ch == '%' || ch == '@' || ch == '`');
        if (!needQuote && !raw.startsWith(" ") && !raw.endsWith(" ")) {
            return raw;
        }
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * 格式化CPU数值
     */
    private String formatCpu(double cpu) {
        if (cpu == Math.rint(cpu)) {
            return String.valueOf((long) cpu);
        }
        return String.valueOf(cpu);
    }
}
