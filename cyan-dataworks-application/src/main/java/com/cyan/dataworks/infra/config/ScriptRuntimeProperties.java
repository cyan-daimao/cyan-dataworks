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
    private String shellImage = "busybox:1.36";

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
}
