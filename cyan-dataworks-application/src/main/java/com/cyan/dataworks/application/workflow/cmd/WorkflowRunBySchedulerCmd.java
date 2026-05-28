package com.cyan.dataworks.application.workflow.cmd;

import com.cyan.dataworks.enums.SchedulerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 调度器触发工作流节点执行命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowRunBySchedulerCmd {

    /** 调度器类型 */
    @NotNull(message = "调度器类型不能为空")
    private SchedulerType schedulerType;

    /** DAG ID */
    @NotBlank(message = "DAG ID不能为空")
    private String dagId;

    /** DAG运行ID */
    @NotBlank(message = "DAG运行ID不能为空")
    private String dagRunId;

    /** 任务ID */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /** 逻辑时间 */
    private LocalDateTime logicalDate;

    /** 重试次数 */
    @NotNull(message = "重试次数不能为空")
    private Integer tryNumber;
}
