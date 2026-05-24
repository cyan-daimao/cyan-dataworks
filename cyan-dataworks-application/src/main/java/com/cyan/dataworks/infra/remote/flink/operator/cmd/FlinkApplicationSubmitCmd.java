package com.cyan.dataworks.infra.remote.flink.operator.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Flink Application 提交命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlinkApplicationSubmitCmd {

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * FlinkDeployment 名称
     */
    private String deploymentName;

    /**
     * ConfigMap 名称
     */
    private String configMapName;

    /**
     * FlinkSQL 内容
     */
    private String sql;

    /**
     * K8s namespace
     */
    private String namespace;

    /**
     * Flink 镜像
     */
    private String image;

    /**
     * Runner jar URI
     */
    private String jarUri;

    /**
     * Runner entry class
     */
    private String entryClass;

    /**
     * 并行度
     */
    private Integer parallelism;

    /**
     * TaskManager内存，单位GB
     */
    private Integer taskManagerMemoryGb;

    /**
     * TaskManager CPU核数
     */
    private Double taskManagerCpu;
}
