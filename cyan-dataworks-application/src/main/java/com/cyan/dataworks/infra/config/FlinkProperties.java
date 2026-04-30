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
         * Flink REST API URL
         */
        private String url;
    }
}
