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
}
