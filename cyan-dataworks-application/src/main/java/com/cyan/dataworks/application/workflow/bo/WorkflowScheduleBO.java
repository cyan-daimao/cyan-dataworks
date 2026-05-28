package com.cyan.dataworks.application.workflow.bo;

import com.cyan.dataworks.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流调度配置业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowScheduleBO {

    /** 主键 */
    private String id;

    /** 工作流ID */
    private String workflowId;

    /** Cron表达式 */
    private String cronExpression;

    /** 是否启用 */
    private Boolean enabled;

    /** 调度器类型 */
    private SchedulerType schedulerType;

    /** 下次执行时间 */
    private LocalDateTime nextExecuteTime;
}
