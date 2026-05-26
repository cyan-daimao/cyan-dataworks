package com.cyan.dataworks.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Flink配置属性
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "flink")
public class FlinkProperties {

    /**
     * REST API地址
     */
    private Rest rest = new Rest();

    /**
     * Iceberg Catalog配置
     */
    private IcebergCatalog icebergCatalog = new IcebergCatalog();

    /**
     * REST配置
     */
    @Data
    public static class Rest {
        /**
         * Flink SQL Gateway REST API URL
         */
        private String url;

        /**
         * 临时执行结果轮询次数
         */
        private Integer previewPollTimes = 10;

        /**
         * 临时执行结果轮询间隔，单位毫秒
         */
        private Integer previewPollIntervalMs = 500;

        /**
         * 临时执行最多读取结果页数
         */
        private Integer previewMaxResultPages = 20;

        /**
         * 临时执行最多返回结果行数
         */
        private Integer previewMaxResultRows = 100;
    }

    /**
     * Iceberg Catalog配置
     */
    @Data
    public static class IcebergCatalog {

        /**
         * 是否自动注入Catalog DDL
         */
        private Boolean enabled = true;

        /**
         * Catalog名称
         */
        private String name = "iceberg";

        /**
         * Catalog类型
         */
        private String type = "iceberg";

        /**
         * Iceberg Catalog类型
         */
        private String catalogType = "rest";

        /**
         * Iceberg REST地址
         */
        private String uri = "http://gravitino-iceberg-rest-server.gravitino.svc.cluster.local:9001/iceberg";

        /**
         * S3地址
         */
        private String s3Endpoint = "http://10.0.0.2:9000";

        /**
         * S3 Access Key
         */
        private String s3AccessKeyId = "rustfsadmin";

        /**
         * S3 Secret Key
         */
        private String s3SecretAccessKey = "rustfsadmin";

        /**
         * 是否使用S3 path-style访问
         */
        private Boolean s3PathStyleAccess = true;
    }
}
