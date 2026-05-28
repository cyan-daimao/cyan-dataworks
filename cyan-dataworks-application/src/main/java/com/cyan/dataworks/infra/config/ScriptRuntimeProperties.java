package com.cyan.dataworks.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Shell和Python脚本运行配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dataworks.script-runtime")
public class ScriptRuntimeProperties {

    /**
     * Kubernetes命名空间
     */
    private String namespace = "dataworks";

    /**
     * Shell任务镜像
     */
    private String shellImage = "python:3.11-slim";

    /**
     * Python任务镜像
     */
    private String pythonImage = "python:3.11-slim";

    /**
     * 默认CPU限制
     */
    private String cpu = "0.5";

    /**
     * 默认内存限制
     */
    private String memory = "512Mi";

    /**
     * 默认超时时间秒数
     */
    private Integer timeoutSeconds = 300;

    /**
     * 任务完成后保留秒数
     */
    private Integer ttlSecondsAfterFinished = 300;

    /**
     * 日志轮询次数
     */
    private Integer pollTimes = 120;

    /**
     * 日志轮询间隔毫秒
     */
    private Integer pollIntervalMs = 1000;

    /**
     * DataWorks回调地址
     */
    private String callbackBaseUrl = "http://cyan-dataworks.pre.svc.cluster.local:8080";

    /**
     * DataWorks回调Token
     */
    private String callbackToken = "dataworks-callback-token";

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
     * 脚本日志Bucket
     */
    private String logBucket = "dataworks-logs";

    /**
     * 脚本日志对象前缀
     */
    private String logBasePrefix = "script";
}
