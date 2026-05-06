package com.cyan.dataworks.application.job_instance;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.domain.job_instance.query.JobInstancePageQuery;

/**
 * 数据加工作业实例应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobInstanceService {

    /**
     * 手动执行作业，生成一个实例
     */
    JobInstanceBO execute(String jobId);

    /**
     * 重试实例（基于原实例重新执行）
     */
    JobInstanceBO retry(String instanceId);

    /**
     * 终止运行中的实例
     */
    JobInstanceBO terminate(String instanceId);

    /**
     * 分页查询实例
     */
    Page<JobInstanceBO> page(JobInstancePageQuery query);

    /**
     * 根据ID查询实例
     */
    JobInstanceBO findById(String id);
}
