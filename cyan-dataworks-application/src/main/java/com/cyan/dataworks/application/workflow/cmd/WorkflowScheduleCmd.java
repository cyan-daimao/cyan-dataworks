package com.cyan.dataworks.application.workflow.cmd;

import com.cyan.dataworks.enums.SchedulerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 工作流调度配置命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowScheduleCmd {

    /** Cron表达式 */
    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    /** 是否启用 */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    /** 调度器类型 */
    private SchedulerType schedulerType;
}
