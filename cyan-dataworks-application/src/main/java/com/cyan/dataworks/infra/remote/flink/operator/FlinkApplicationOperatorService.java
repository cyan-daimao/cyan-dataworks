package com.cyan.dataworks.infra.remote.flink.operator;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.infra.remote.flink.operator.bo.FlinkApplicationBO;
import com.cyan.dataworks.infra.remote.flink.operator.cmd.FlinkApplicationSubmitCmd;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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

    @Value("${flink.operator.namespace:flink}")
    private String defaultNamespace;

    @Value("${flink.operator.image:harbor.cyan.com/cyan/flink-sql:2.0.1}")
    private String defaultImage;

    @Value("${flink.operator.jar-uri:local:///opt/flink/lib/sql-runner.jar}")
    private String defaultJarUri;

    @Value("${flink.operator.entry-class:com.cyan.dataman.infra.flink.SqlRunner}")
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
            k8sClient.load(new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
                    .inNamespace(namespace)
                    .createOrReplace();
            log.info("FlinkDeployment 创建/更新成功: {} in namespace {}", deploymentName, namespace);

            return new FlinkApplicationBO()
                    .setDeploymentName(deploymentName)
                    .setConfigMapName(configMapName)
                    .setNamespace(namespace)
                    .setStatus("RUNNING")
                    .setMessage("Flink Application 提交成功");
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
        try {
            k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .delete();
            log.info("FlinkDeployment 删除成功: {}", deploymentName);
        } catch (Exception e) {
            log.warn("删除 FlinkDeployment 失败: {}, error: {}", deploymentName, e.getMessage());
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

    private void createOrUpdateConfigMap(String name, String sql, String namespace) {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .addToData("job.sql", sql)
                .build();
        k8sClient.configMaps().inNamespace(namespace).createOrReplace(configMap);
    }

    private String buildFlinkDeploymentYaml(FlinkApplicationSubmitCmd cmd, String namespace) {
        String deploymentName = cmd.getDeploymentName();
        String configMapName = cmd.getConfigMapName();
        String image = cmd.getImage() != null ? cmd.getImage() : defaultImage;
        String jarUri = cmd.getJarUri() != null ? cmd.getJarUri() : defaultJarUri;
        String entryClass = cmd.getEntryClass() != null ? cmd.getEntryClass() : defaultEntryClass;
        int parallelism = cmd.getParallelism() != null ? cmd.getParallelism() : defaultParallelism;

        return String.format("""
                apiVersion: flink.apache.org/v1beta1
                kind: FlinkDeployment
                metadata:
                  name: %s
                  namespace: %s
                spec:
                  serviceAccount: flink
                  image: %s
                  flinkVersion: v2_0
                  jobManager:
                    resource:
                      memory: "1g"
                      cpu: 0.5
                  taskManager:
                    resource:
                      memory: "1g"
                      cpu: 0.5
                  flinkConfiguration:
                    state.backend.type: rocksdb
                    classloader.parent-first-patterns.additional: com.codahale.metrics
                    state.checkpoints.dir: s3://flink/checkpoints/cyan-dataworks
                    state.savepoints.dir: s3://flink/savepoints/cyan-dataworks
                    execution.checkpointing.interval: 60s
                    execution.checkpointing.timeout: 600s
                    execution.checkpointing.max-concurrent-checkpoints: 1
                    execution.checkpointing.min-pause: 500ms
                    execution.checkpointing.mode: EXACTLY_ONCE
                    s3.endpoint: %s
                    s3.access-key: %s
                    s3.secret-key: %s
                    s3.path.style.access: true
                  job:
                    jarURI: %s
                    entryClass: %s
                    args:
                      - "/opt/flink/sql/job.sql"
                    parallelism: %d
                    upgradeMode: last-state
                    state: running
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
                deploymentName, namespace, image,
                rustfsEndpoint, rustfsAccessKey, rustfsSecretKey,
                jarUri, entryClass, parallelism,
                configMapName);
    }
}
