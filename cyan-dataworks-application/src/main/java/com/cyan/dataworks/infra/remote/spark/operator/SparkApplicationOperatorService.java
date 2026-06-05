package com.cyan.dataworks.infra.remote.spark.operator;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.infra.config.SparkOperatorProperties;
import com.cyan.dataworks.infra.remote.spark.operator.bo.SparkApplicationBO;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ContainerResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spark Operator Application提交服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class SparkApplicationOperatorService {

    /**
     * SparkApplication Kind
     */
    private static final String SPARK_APPLICATION_KIND = "SparkApplication";

    /**
     * SQL文件名
     */
    private static final String SQL_FILE_NAME = "job.sql";

    /**
     * Kubernetes客户端
     */
    private final KubernetesClient kubernetesClient;

    /**
     * Spark Operator配置
     */
    private final SparkOperatorProperties properties;

    public SparkApplicationOperatorService(KubernetesClient kubernetesClient, SparkOperatorProperties properties) {
        this.kubernetesClient = kubernetesClient;
        this.properties = properties;
    }

    /**
     * 提交Spark SQL任务
     *
     * @param instanceId 实例ID
     * @param sql        SQL内容
     * @param configJson 任务配置
     * @return SparkApplication业务对象
     */
    public SparkApplicationBO submitSparkSql(String instanceId, String sql, String configJson) {
        SparkRuntimeConfig runtimeConfig = parseRuntimeConfig(configJson);
        String namespace = runtimeConfig.namespace();
        String applicationName = toK8sName("dataworks-spark-" + instanceId);
        String configMapName = toK8sName("dataworks-spark-sql-" + instanceId);
        try {
            createOrUpdateConfigMap(configMapName, sql, namespace, instanceId, applicationName);
            String yaml = buildSparkApplicationYaml(instanceId, applicationName, configMapName, runtimeConfig);
            GenericKubernetesResource sparkApplication = Serialization.unmarshal(yaml, GenericKubernetesResource.class);
            kubernetesClient.resource(sparkApplication).inNamespace(namespace).serverSideApply();
            log.info("SparkApplication提交成功: instanceId={}, namespace={}, applicationName={}, configMapName={}",
                    instanceId, namespace, applicationName, configMapName);
            return new SparkApplicationBO()
                    .setApplicationName(applicationName)
                    .setNamespace(namespace)
                    .setConfigMapName(configMapName)
                    .setState("SUBMITTED")
                    .setRunning(true)
                    .setCompleted(false)
                    .setFailed(false)
                    .setMessage("SparkApplication提交成功");
        } catch (Exception e) {
            log.error("SparkApplication提交失败: instanceId={}, applicationName={}", instanceId, applicationName, e);
            throw new SilentException("SparkApplication提交失败: " + e.getMessage());
        }
    }

    /**
     * 查询SparkApplication状态
     *
     * @param applicationName SparkApplication名称
     * @param namespace       命名空间
     * @return SparkApplication业务对象
     */
    public SparkApplicationBO getStatus(String applicationName, String namespace) {
        String actualNamespace = normalizeNamespace(namespace);
        GenericKubernetesResource resource = get(applicationName, actualNamespace);
        if (resource == null) {
            return new SparkApplicationBO()
                    .setApplicationName(applicationName)
                    .setNamespace(actualNamespace)
                    .setState("NOT_FOUND")
                    .setRunning(false)
                    .setCompleted(false)
                    .setFailed(true)
                    .setMessage("SparkApplication不存在或已被删除");
        }
        Map<String, Object> additionalProperties = Optional.ofNullable(resource.getAdditionalProperties()).orElse(Map.of());
        Map<String, Object> status = asMap(additionalProperties.get("status"));
        Map<String, Object> applicationState = asMap(status.get("applicationState"));
        String state = Optional.ofNullable(applicationState.get("state"))
                .map(String::valueOf)
                .orElse("UNKNOWN");
        String errorMessage = Optional.ofNullable(applicationState.get("errorMessage"))
                .map(String::valueOf)
                .orElse("");
        String driverPodName = resolveDriverPodName(status, applicationName, actualNamespace);
        return new SparkApplicationBO()
                .setApplicationName(applicationName)
                .setNamespace(actualNamespace)
                .setConfigMapName(toK8sName("dataworks-spark-sql-" + resolveInstanceId(resource)))
                .setDriverPodName(driverPodName)
                .setState(state)
                .setRunning(isRunningState(state))
                .setCompleted(isCompletedState(state))
                .setFailed(isFailedState(state))
                .setMessage(errorMessage);
    }

    /**
     * 删除SparkApplication及SQL ConfigMap
     *
     * @param applicationName SparkApplication名称
     * @param namespace       命名空间
     * @param configMapName   ConfigMap名称
     */
    public void delete(String applicationName, String namespace, String configMapName) {
        String actualNamespace = normalizeNamespace(namespace);
        if (applicationName != null && !applicationName.isBlank()) {
            try {
                kubernetesClient.genericKubernetesResources(properties.getApiVersion(), SPARK_APPLICATION_KIND)
                        .inNamespace(actualNamespace)
                        .withName(applicationName)
                        .delete();
                log.info("SparkApplication删除成功: namespace={}, applicationName={}", actualNamespace, applicationName);
            } catch (Exception e) {
                log.warn("SparkApplication删除失败: namespace={}, applicationName={}, error={}",
                        actualNamespace, applicationName, e.getMessage());
            }
        }
        if (configMapName != null && !configMapName.isBlank()) {
            try {
                kubernetesClient.configMaps().inNamespace(actualNamespace).withName(configMapName).delete();
                log.info("Spark SQL ConfigMap删除成功: namespace={}, configMapName={}", actualNamespace, configMapName);
            } catch (Exception e) {
                log.warn("Spark SQL ConfigMap删除失败: namespace={}, configMapName={}, error={}",
                        actualNamespace, configMapName, e.getMessage());
            }
        }
    }

    /**
     * 读取Driver Pod日志
     *
     * @param applicationName SparkApplication名称
     * @param namespace       命名空间
     * @param tailLines       尾部行数
     * @return 日志内容
     */
    public String readDriverLog(String applicationName, String namespace, Integer tailLines) {
        if (applicationName == null || applicationName.isBlank()) {
            return "";
        }
        String actualNamespace = normalizeNamespace(namespace);
        Pod driverPod = findDriverPod(applicationName, actualNamespace);
        if (driverPod == null) {
            return "";
        }
        String podName = Optional.ofNullable(driverPod.getMetadata()).map(metadata -> metadata.getName()).orElse("");
        String containerName = resolveContainerName(driverPod);
        try {
            ContainerResource container = kubernetesClient.pods()
                    .inNamespace(actualNamespace)
                    .withName(podName)
                    .inContainer(containerName);
            int actualTailLines = tailLines == null || tailLines <= 0 ? 1000 : tailLines;
            String logs = container.tailingLines(actualTailLines).getLog();
            return logs == null ? "" : logs;
        } catch (Exception e) {
            log.warn("读取Spark Driver日志失败: namespace={}, pod={}, container={}, error={}",
                    actualNamespace, podName, containerName, e.getMessage());
            return "读取Spark Driver日志失败: " + e.getMessage();
        }
    }

    private GenericKubernetesResource get(String applicationName, String namespace) {
        try {
            return kubernetesClient.genericKubernetesResources(properties.getApiVersion(), SPARK_APPLICATION_KIND)
                    .inNamespace(namespace)
                    .withName(applicationName)
                    .get();
        } catch (Exception e) {
            log.warn("查询SparkApplication失败: namespace={}, applicationName={}, error={}",
                    namespace, applicationName, e.getMessage());
            return null;
        }
    }

    private void createOrUpdateConfigMap(String name, String sql, String namespace, String instanceId, String applicationName) {
        Map<String, String> labels = Map.of(
                "app", "cyan-dataworks",
                "dataworks-instance-id", instanceId,
                "dataworks-task-type", "spark-sql",
                "sparkapplication", applicationName
        );
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .addToData(SQL_FILE_NAME, sql)
                .build();
        kubernetesClient.resource(configMap).inNamespace(namespace).serverSideApply();
    }

    private String buildSparkApplicationYaml(String instanceId,
                                             String applicationName,
                                             String configMapName,
                                             SparkRuntimeConfig runtimeConfig) {
        String sqlFilePath = runtimeConfig.sqlMountPath().replaceAll("/+$", "") + "/" + SQL_FILE_NAME;
        Map<String, String> sparkConf = buildSparkConf(runtimeConfig);
        String sparkConfYaml = sparkConf.entrySet().stream()
                .map(entry -> "    " + yamlQuote(entry.getKey()) + ": " + yamlQuote(entry.getValue()))
                .collect(Collectors.joining("\n"));
        String imagePullSecretsYaml = runtimeConfig.imagePullSecret() == null || runtimeConfig.imagePullSecret().isBlank()
                ? ""
                : "  imagePullSecrets:\n    - name: " + runtimeConfig.imagePullSecret() + "\n";

        return """
                apiVersion: %s
                kind: SparkApplication
                metadata:
                  name: %s
                  namespace: %s
                  labels:
                    app: cyan-dataworks
                    dataworks-instance-id: %s
                    dataworks-task-type: spark-sql
                spec:
                  type: %s
                  mode: cluster
                  image: %s
                  imagePullPolicy: %s
                %s  sparkVersion: %s
                  mainClass: %s
                  mainApplicationFile: %s
                  arguments:
                    - %s
                  timeToLiveSeconds: %d
                  restartPolicy:
                    type: Never
                  sparkConf:
                %s
                  volumes:
                    - name: sql-volume
                      configMap:
                        name: %s
                  driver:
                    cores: %d
                    coreLimit: %s
                    memory: %s
                    serviceAccount: %s
                    labels:
                      dataworks-instance-id: %s
                    volumeMounts:
                      - name: sql-volume
                        mountPath: %s
                  executor:
                    instances: %d
                    cores: %d
                    coreLimit: %s
                    memory: %s
                    labels:
                      dataworks-instance-id: %s
                    volumeMounts:
                      - name: sql-volume
                        mountPath: %s
                """.formatted(
                runtimeConfig.apiVersion(),
                applicationName,
                runtimeConfig.namespace(),
                instanceId,
                runtimeConfig.type(),
                runtimeConfig.image(),
                runtimeConfig.imagePullPolicy(),
                imagePullSecretsYaml,
                runtimeConfig.sparkVersion(),
                runtimeConfig.mainClass(),
                runtimeConfig.mainApplicationFile(),
                yamlQuote(sqlFilePath),
                runtimeConfig.ttlSecondsAfterFinished(),
                sparkConfYaml,
                configMapName,
                runtimeConfig.driverCores(),
                yamlQuote(runtimeConfig.driverCoreLimit()),
                yamlQuote(runtimeConfig.driverMemory()),
                runtimeConfig.serviceAccount(),
                instanceId,
                runtimeConfig.sqlMountPath(),
                runtimeConfig.executorInstances(),
                runtimeConfig.executorCores(),
                yamlQuote(runtimeConfig.executorCoreLimit()),
                yamlQuote(runtimeConfig.executorMemory()),
                instanceId,
                runtimeConfig.sqlMountPath());
    }

    private Map<String, String> buildSparkConf(SparkRuntimeConfig runtimeConfig) {
        Map<String, String> sparkConf = new LinkedHashMap<>();
        sparkConf.put("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions");
        sparkConf.put("spark.sql.catalog.iceberg", "org.apache.iceberg.spark.SparkCatalog");
        sparkConf.put("spark.sql.catalog.iceberg.catalog-impl", "org.apache.iceberg.rest.RESTCatalog");
        sparkConf.put("spark.sql.catalog.iceberg.uri", runtimeConfig.icebergRestUri());
        sparkConf.put("spark.sql.catalog.iceberg.io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        sparkConf.put("spark.sql.catalog.iceberg.s3.endpoint", runtimeConfig.rustfsEndpoint());
        sparkConf.put("spark.sql.catalog.iceberg.s3.access-key-id", runtimeConfig.rustfsAccessKey());
        sparkConf.put("spark.sql.catalog.iceberg.s3.secret-access-key", runtimeConfig.rustfsSecretKey());
        sparkConf.put("spark.sql.catalog.iceberg.s3.path-style-access", "true");
        sparkConf.put("spark.sql.defaultCatalog", "iceberg");
        sparkConf.putAll(runtimeConfig.sparkConf());
        return sparkConf;
    }

    private SparkRuntimeConfig parseRuntimeConfig(String configJson) {
        SparkRuntimeConfig defaults = new SparkRuntimeConfig(
                properties.getNamespace(),
                properties.getApiVersion(),
                properties.getImage(),
                properties.getImagePullPolicy(),
                Optional.ofNullable(properties.getImagePullSecret()).orElse(""),
                properties.getType(),
                properties.getSparkVersion(),
                properties.getMainApplicationFile(),
                properties.getMainClass(),
                properties.getServiceAccount(),
                properties.getDriverCores(),
                properties.getDriverMemory(),
                properties.getDriverCoreLimit(),
                properties.getExecutorInstances(),
                properties.getExecutorCores(),
                properties.getExecutorMemory(),
                properties.getExecutorCoreLimit(),
                properties.getTtlSecondsAfterFinished(),
                properties.getSqlMountPath(),
                properties.getIcebergRestUri(),
                properties.getRustfsEndpoint(),
                properties.getRustfsAccessKey(),
                properties.getRustfsSecretKey(),
                Optional.ofNullable(properties.getSparkConf()).orElse(Map.of())
        );
        if (configJson == null || configJson.isBlank()) {
            return defaults;
        }
        try {
            JSONObject root = JSON.parseObject(configJson);
            JSONObject spark = root == null ? null : root.getJSONObject("spark");
            if (spark == null) {
                return defaults;
            }
            Map<String, String> sparkConf = new LinkedHashMap<>(defaults.sparkConf());
            JSONObject customSparkConf = spark.getJSONObject("sparkConf");
            if (customSparkConf != null) {
                customSparkConf.forEach((key, value) -> {
                    if (value != null) {
                        sparkConf.put(key, String.valueOf(value));
                    }
                });
            }
            return new SparkRuntimeConfig(
                    stringValue(spark, "namespace", defaults.namespace()),
                    stringValue(spark, "apiVersion", defaults.apiVersion()),
                    stringValue(spark, "image", defaults.image()),
                    stringValue(spark, "imagePullPolicy", defaults.imagePullPolicy()),
                    stringValue(spark, "imagePullSecret", defaults.imagePullSecret()),
                    stringValue(spark, "type", defaults.type()),
                    stringValue(spark, "sparkVersion", defaults.sparkVersion()),
                    stringValue(spark, "mainApplicationFile", defaults.mainApplicationFile()),
                    stringValue(spark, "mainClass", defaults.mainClass()),
                    stringValue(spark, "serviceAccount", defaults.serviceAccount()),
                    integerValue(spark, "driverCores", defaults.driverCores()),
                    stringValue(spark, "driverMemory", defaults.driverMemory()),
                    stringValue(spark, "driverCoreLimit", defaults.driverCoreLimit()),
                    integerValue(spark, "executorInstances", defaults.executorInstances()),
                    integerValue(spark, "executorCores", defaults.executorCores()),
                    stringValue(spark, "executorMemory", defaults.executorMemory()),
                    stringValue(spark, "executorCoreLimit", defaults.executorCoreLimit()),
                    integerValue(spark, "ttlSecondsAfterFinished", defaults.ttlSecondsAfterFinished()),
                    stringValue(spark, "sqlMountPath", defaults.sqlMountPath()),
                    stringValue(spark, "icebergRestUri", defaults.icebergRestUri()),
                    stringValue(spark, "rustfsEndpoint", defaults.rustfsEndpoint()),
                    stringValue(spark, "rustfsAccessKey", defaults.rustfsAccessKey()),
                    stringValue(spark, "rustfsSecretKey", defaults.rustfsSecretKey()),
                    sparkConf
            );
        } catch (Exception e) {
            log.warn("解析Spark运行配置失败，将使用默认配置: {}", e.getMessage());
            return defaults;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String resolveDriverPodName(Map<String, Object> status, String applicationName, String namespace) {
        Map<String, Object> driverInfo = asMap(status.get("driverInfo"));
        String driverPodName = Optional.ofNullable(driverInfo.get("podName")).map(String::valueOf).orElse("");
        if (!driverPodName.isBlank()) {
            return driverPodName;
        }
        return Optional.ofNullable(findDriverPod(applicationName, namespace))
                .map(Pod::getMetadata)
                .map(metadata -> metadata.getName())
                .orElse("");
    }

    private Pod findDriverPod(String applicationName, String namespace) {
        List<Pod> pods = Optional.ofNullable(kubernetesClient.pods().inNamespace(namespace).list().getItems()).orElse(List.of());
        return pods.stream()
                .filter(pod -> matchDriverPod(pod, applicationName))
                .sorted(Comparator.comparing(pod -> Optional.ofNullable(pod.getMetadata()).map(metadata -> metadata.getCreationTimestamp()).orElse("")))
                .findFirst()
                .orElse(null);
    }

    private boolean matchDriverPod(Pod pod, String applicationName) {
        if (applicationName == null || applicationName.isBlank()) {
            return false;
        }
        Map<String, String> labels = Optional.ofNullable(pod.getMetadata()).map(metadata -> metadata.getLabels()).orElse(Map.of());
        String name = Optional.ofNullable(pod.getMetadata()).map(metadata -> metadata.getName()).orElse("");
        String haystack = (name + " " + labels).toLowerCase(Locale.ROOT);
        return haystack.contains(applicationName.toLowerCase(Locale.ROOT)) && haystack.contains("driver");
    }

    private String resolveContainerName(Pod pod) {
        return Optional.ofNullable(pod.getSpec())
                .map(spec -> spec.getContainers())
                .orElse(List.of())
                .stream()
                .findFirst()
                .map(container -> container.getName())
                .orElse("spark-kubernetes-driver");
    }

    private String resolveInstanceId(GenericKubernetesResource resource) {
        return Optional.ofNullable(resource.getMetadata())
                .map(metadata -> metadata.getLabels())
                .map(labels -> labels.get("dataworks-instance-id"))
                .orElse("");
    }

    private boolean isRunningState(String state) {
        String normalized = state == null ? "" : state.toUpperCase(Locale.ROOT);
        return normalized.isBlank()
                || normalized.equals("UNKNOWN")
                || normalized.equals("NEW")
                || normalized.equals("SUBMITTED")
                || normalized.equals("RUNNING")
                || normalized.equals("SUBMISSION_PENDING");
    }

    private boolean isCompletedState(String state) {
        return "COMPLETED".equalsIgnoreCase(state);
    }

    private boolean isFailedState(String state) {
        String normalized = state == null ? "" : state.toUpperCase(Locale.ROOT);
        return normalized.contains("FAILED") || normalized.equals("FAILING") || normalized.equals("INVALIDATING");
    }

    private String normalizeNamespace(String namespace) {
        return Optional.ofNullable(namespace).filter(value -> !value.isBlank()).orElse(properties.getNamespace());
    }

    private String stringValue(JSONObject object, String key, String defaultValue) {
        return Optional.ofNullable(object.getString(key)).filter(value -> !value.isBlank()).orElse(defaultValue);
    }

    private Integer integerValue(JSONObject object, String key, Integer defaultValue) {
        return Optional.ofNullable(object.getInteger(key)).filter(value -> value > 0).orElse(defaultValue);
    }

    private String yamlQuote(String value) {
        String escaped = Optional.ofNullable(value).orElse("").replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private String toK8sName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
        return normalized.length() > 63 ? normalized.substring(0, 63).replaceAll("-$", "") : normalized;
    }

    private record SparkRuntimeConfig(String namespace,
                                      String apiVersion,
                                      String image,
                                      String imagePullPolicy,
                                      String imagePullSecret,
                                      String type,
                                      String sparkVersion,
                                      String mainApplicationFile,
                                      String mainClass,
                                      String serviceAccount,
                                      Integer driverCores,
                                      String driverMemory,
                                      String driverCoreLimit,
                                      Integer executorInstances,
                                      Integer executorCores,
                                      String executorMemory,
                                      String executorCoreLimit,
                                      Integer ttlSecondsAfterFinished,
                                      String sqlMountPath,
                                      String icebergRestUri,
                                      String rustfsEndpoint,
                                      String rustfsAccessKey,
                                      String rustfsSecretKey,
                                      Map<String, String> sparkConf) {
    }
}
