package com.cyan.dataworks.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spark Operator配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dataworks.spark-operator")
public class SparkOperatorProperties {

    /**
     * SparkApplication命名空间
     */
    private String namespace = "spark";

    /**
     * SparkApplication API版本
     */
    private String apiVersion = "sparkoperator.k8s.io/v1beta2";

    /**
     * SparkApplication镜像
     */
    private String image = "harbor.cyan.com/cyan/dataworks-spark-sql:4.0.2";

    /**
     * 镜像拉取策略
     */
    private String imagePullPolicy = "Always";

    /**
     * 镜像拉取Secret
     */
    private String imagePullSecret = "harbor-secret";

    /**
     * SparkApplication类型
     */
    private String type = "Java";

    /**
     * Spark版本
     */
    private String sparkVersion = "4.0.2";

    /**
     * Runner主文件
     */
    private String mainApplicationFile = "local:///opt/spark/app/dataworks-spark-runner.jar";

    /**
     * Runner入口类
     */
    private String mainClass = "com.cyan.dataworks.spark.SqlRunner";

    /**
     * Driver和Executor服务账号
     */
    private String serviceAccount = "spark-operator-spark";

    /**
     * Driver CPU核心数
     */
    private Integer driverCores = 1;

    /**
     * Driver内存
     */
    private String driverMemory = "1g";

    /**
     * Driver core limit
     */
    private String driverCoreLimit = "1200m";

    /**
     * Executor数量
     */
    private Integer executorInstances = 1;

    /**
     * Executor CPU核心数
     */
    private Integer executorCores = 1;

    /**
     * Executor内存
     */
    private String executorMemory = "2g";

    /**
     * Executor core limit
     */
    private String executorCoreLimit = "1200m";

    /**
     * SparkApplication完成后保留秒数
     */
    private Integer ttlSecondsAfterFinished = 300;

    /**
     * SQL文件挂载目录
     */
    private String sqlMountPath = "/opt/spark/work-dir/dataworks";

    /**
     * Iceberg REST Catalog地址
     */
    private String icebergRestUri = "http://gravitino-iceberg-rest-server.gravitino.svc.cluster.local:9001/iceberg";

    /**
     * RustFS访问地址
     */
    private String rustfsEndpoint = "http://10.0.0.2:9000";

    /**
     * RustFS Access Key
     */
    private String rustfsAccessKey = "rustfsadmin";

    /**
     * RustFS Secret Key
     */
    private String rustfsSecretKey = "rustfsadmin";

    /**
     * 附加Spark配置
     */
    private Map<String, String> sparkConf = new LinkedHashMap<>();
}
