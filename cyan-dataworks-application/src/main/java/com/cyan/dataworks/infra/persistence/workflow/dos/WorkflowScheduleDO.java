package com.cyan.dataworks.infra.persistence.workflow.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流调度配置数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_workflow_schedule")
public class WorkflowScheduleDO {

    /** 主键 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 工作流ID */
    @TableField("workflow_id")
    private Long workflowId;

    /** Cron表达式 */
    @TableField("cron_expression")
    private String cronExpression;

    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;

    /** 调度器类型 */
    @TableField("scheduler_type")
    private SchedulerType schedulerType;

    /** 下次执行时间 */
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    /** 创建人 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新人 */
    @TableField("updated_by")
    private String updatedBy;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 删除时间 */
    @TableField("deleted_at")
    @TableLogic(value = "null", delval = "now(6)")
    private LocalDateTime deletedAt;
}
