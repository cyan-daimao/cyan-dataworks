package com.cyan.dataworks.domain.job_instance.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.query.JobInstancePageQuery;

/**
 * 作业实例仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobInstanceRepository {

    /**
     * 分页查询实例
     */
    Page<JobInstance> page(JobInstancePageQuery query);

    /**
     * 根据ID查询实例
     */
    JobInstance findById(String id);

    /**
     * 保存实例
     */
    JobInstance save(JobInstance jobInstance);

    /**
     * 更新实例
     */
    JobInstance updateById(JobInstance jobInstance);

    /**
     * 根据作业ID删除实例
     */
    void deleteByJobId(String jobId);

    /**
     * 根据作业ID查询最近一个实例
     *
     * @param jobId 作业ID
     * @return 最近的实例
     */
    JobInstance findLatestByJobId(String jobId);

    /**
     * 根据调度器追踪信息查询实例
     *
     * @param schedulerDagRunId 调度器DAG运行ID
     * @param schedulerTaskId   调度器任务ID
     * @param schedulerTryNumber 调度器重试次数
     * @return 作业实例
     */
    JobInstance findBySchedulerTrace(String schedulerDagRunId, String schedulerTaskId, Integer schedulerTryNumber);
}
