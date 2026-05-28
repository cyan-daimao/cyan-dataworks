package com.cyan.dataworks.infra.persistence.job_instance.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业实例数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_job_instance")
public class JobInstanceDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 作业ID
     */
    @TableField(value = "job_id")
    private Long jobId;

    /**
     * 工作流实例ID
     */
    @TableField(value = "workflow_instance_id")
    private Long workflowInstanceId;

    /**
     * 工作流节点ID
     */
    @TableField(value = "workflow_node_id")
    private Long workflowNodeId;

    /**
     * 作业名称
     */
    @TableField(value = "job_name")
    private String jobName;

    /**
     * 引擎类型
     */
    @TableField(value = "engine_type")
    private EngineType engineType;

    /**
     * 任务内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 执行状态
     */
    @TableField(value = "status")
    private ExecutionStatus status;

    /**
     * 耗时（毫秒）
     */
    @TableField(value = "cost_time_ms")
    private Long costTimeMs;

    /**
     * 结果数据（JSON）
     */
    @TableField(value = "result_data")
    private String resultData;

    /**
     * 错误信息
     */
    @TableField(value = "error_message")
    private String errorMessage;

    /**
     * Flink Application名称
     */
    @TableField(value = "application_name")
    private String applicationName;

    /**
     * Flink Application命名空间
     */
    @TableField(value = "application_namespace")
    private String applicationNamespace;

    /**
     * Flink ConfigMap名称
     */
    @TableField(value = "config_map_name")
    private String configMapName;

    /**
     * JobManager Pod名称
     */
    @TableField(value = "job_manager_pod_name")
    private String jobManagerPodName;

    /**
     * TaskManager Pod名称列表（JSON）
     */
    @TableField(value = "task_manager_pod_names")
    private String taskManagerPodNames;

    /**
     * 调度器类型
     */
    @TableField(value = "scheduler_type")
    private SchedulerType schedulerType;

    /**
     * 调度器DAG ID
     */
    @TableField(value = "scheduler_dag_id")
    private String schedulerDagId;

    /**
     * 调度器DAG运行ID
     */
    @TableField(value = "scheduler_dag_run_id")
    private String schedulerDagRunId;

    /**
     * 调度器任务ID
     */
    @TableField(value = "scheduler_task_id")
    private String schedulerTaskId;

    /**
     * 调度器重试次数
     */
    @TableField(value = "scheduler_try_number")
    private Integer schedulerTryNumber;

    /**
     * 创建人
     */
    @TableField(value = "created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    @TableField(value = "updated_by")
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
