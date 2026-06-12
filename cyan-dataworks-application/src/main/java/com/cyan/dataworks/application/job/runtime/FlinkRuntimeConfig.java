package com.cyan.dataworks.application.job.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flink运行配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlinkRuntimeConfig {

    /**
     * TaskManager内存，单位GB
     */
    private Integer taskManagerMemoryGb;

    /**
     * TaskManager CPU核数
     */
    private Double taskManagerCpu;

    /**
     * Flink作业并行度
     */
    private Integer parallelism;

    /**
     * JobManager内存，单位GB
     */
    private Integer jobManagerMemoryGb;

    /**
     * JobManager CPU核数
     */
    private Double jobManagerCpu;

    /**
     * Flink版本（FlinkDeployment spec.flinkVersion）
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
