package com.cyan.dataworks.adapter.workflow.http.dto;

import com.cyan.dataworks.enums.SchedulerType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流调度配置DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowScheduleDTO {

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecuteTime;
}
