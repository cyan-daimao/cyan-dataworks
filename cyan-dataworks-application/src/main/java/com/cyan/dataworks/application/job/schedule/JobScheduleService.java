package com.cyan.dataworks.application.job.schedule;

import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import com.cyan.dataworks.application.job.schedule.cmd.JobScheduleCmd;

/**
 * 作业调度配置应用服务接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobScheduleService {

    /**
     * 根据作业ID查询调度配置
     */
    JobScheduleBO findByJobId(String jobId);

    /**
     * 保存或更新调度配置
     */
    JobScheduleBO saveOrUpdate(String jobId, JobScheduleCmd cmd);
}
