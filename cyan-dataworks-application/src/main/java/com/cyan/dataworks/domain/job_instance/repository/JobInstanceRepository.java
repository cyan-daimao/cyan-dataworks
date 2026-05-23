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
}
