package com.cyan.dataworks.client.job_schedule.request;

import com.cyan.dataworks.client.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 作业调度配置保存请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobScheduleSaveRequest {

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 调度器类型
     */
    private SchedulerType schedulerType;
}
