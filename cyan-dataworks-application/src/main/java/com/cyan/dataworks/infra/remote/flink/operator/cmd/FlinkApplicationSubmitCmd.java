package com.cyan.dataworks.infra.remote.flink.operator.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

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
     * Flink任务内容
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

    /**
     * JobManager内存，单位GB
     */
    private Integer jobManagerMemoryGb;

    /**
     * JobManager CPU核数
     */
    private Double jobManagerCpu;

    /**
     * Flink版本（FlinkDeployment spec.flinkVersion，如 v2_0）
     */
    private String flinkVersion;

    /**
     * 升级模式（last-state / savepoint / stateless）
     */
    private String upgradeMode;

    /**
     * 期望状态（running / suspended）
     */
    private String state;

    /**
     * 检查点间隔
     */
    private String checkpointInterval;

    /**
     * 检查点超时
     */
    private String checkpointTimeout;

    /**
     * 检查点最大并发数
     */
    private Integer checkpointMaxConcurrent;

    /**
     * 检查点最小间隔
     */
    private String checkpointMinPause;

    /**
     * 检查点模式（EXACTLY_ONCE / AT_LEAST_ONCE）
     */
    private String checkpointMode;

    /**
     * 状态后端类型（rocksdb / hashmap）
     */
    private String stateBackendType;

    /**
     * 用户自定义 flinkConfiguration（与默认配置合并，键冲突时以用户值为准）
     */
    private Map<String, String> extraFlinkConfiguration = new LinkedHashMap<>();
}

