package com.cyan.dataworks.adapter.job_instance.http.dto;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.SchedulerType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业实例 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 工作流实例ID
     */
    private String workflowInstanceId;

    /**
     * 工作流节点ID
     */
    private String workflowNodeId;

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 耗时（毫秒）
     */
    private Long costTimeMs;

    /**
     * 结果数据（JSON）
     */
    private String resultData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * Flink Application名称
     */
    private String applicationName;

    /**
     * Flink Application命名空间
     */
    private String applicationNamespace;

    /**
     * Flink ConfigMap名称
     */
    private String configMapName;

    /**
     * JobManager Pod名称
     */
    private String jobManagerPodName;

    /**
     * TaskManager Pod名称列表（JSON）
     */
    private String taskManagerPodNames;

    /**
     * 调度器类型
     */
    private SchedulerType schedulerType;

    /**
     * 调度器DAG ID
     */
    private String schedulerDagId;

    /**
     * 调度器DAG运行ID
     */
    private String schedulerDagRunId;

    /**
     * 调度器任务ID
     */
    private String schedulerTaskId;

    /**
     * 调度器重试次数
     */
    private Integer schedulerTryNumber;

    /**
     * 运行时Kubernetes Job名称
     */
    private String runtimeJobName;

    /**
     * RustFS日志对象Key
     */
    private String logObjectKey;

    /**
     * 运行开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * 运行结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    /**
     * 回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callbackAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
