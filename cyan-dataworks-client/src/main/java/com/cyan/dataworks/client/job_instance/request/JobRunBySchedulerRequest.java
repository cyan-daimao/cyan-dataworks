package com.cyan.dataworks.client.job_instance.request;

import com.cyan.dataworks.client.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 调度器触发作业执行请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobRunBySchedulerRequest {

    /**
     * 调度器类型
     */
    private SchedulerType schedulerType;

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * DAG运行ID
     */
    private String dagRunId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 逻辑时间
     */
    private LocalDateTime logicalDate;

    /**
     * 重试次数
     */
    private Integer tryNumber;
}
