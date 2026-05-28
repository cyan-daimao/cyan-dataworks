package com.cyan.dataworks.domain.job.schedule.repository;

import com.cyan.dataworks.domain.job.schedule.JobSchedule;

import java.util.List;

/**
 * 作业调度配置仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobScheduleRepository {

    /**
     * 根据作业ID查询调度配置
     */
    JobSchedule findByJobId(String jobId);

    /**
     * 保存调度配置
     */
    JobSchedule save(JobSchedule jobSchedule);

    /**
     * 更新调度配置
     */
    JobSchedule updateById(JobSchedule jobSchedule);

    /**
     * 根据作业ID删除调度配置
     */
    void deleteByJobId(String jobId);

    /**
     * 查询已启用的调度配置
     */
    List<JobSchedule> listEnabled();

    /**
     * 查询Airflow调度配置
     */
    List<JobSchedule> listAirflow();
}
