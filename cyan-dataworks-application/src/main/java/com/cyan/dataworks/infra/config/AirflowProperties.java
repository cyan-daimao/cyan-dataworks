package com.cyan.dataworks.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Airflow集成配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "airflow")
public class AirflowProperties {

    /**
     * 是否启用Airflow集成
     */
    private Boolean enabled = false;

    /**
     * Airflow REST API地址
     */
    private String baseUrl = "";

    /**
     * DAG ID前缀
     */
    private String dagPrefix = "dataworks";

    /**
     * 用户名
     */
    private String username = "";

    /**
     * 密码
     */
    private String password = "";

    /**
     * 访问令牌
     */
    private String token = "";
}
