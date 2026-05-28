package com.cyan.dataworks.application.job.dependency;

import com.cyan.dataworks.application.job.dependency.bo.JobDependencyBO;
import com.cyan.dataworks.application.job.dependency.bo.JobLineageBO;
import com.cyan.dataworks.application.job.dependency.cmd.JobDependencyCmd;

/**
 * 作业依赖应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobDependencyService {

    /**
     * 查询作业依赖
     */
    JobDependencyBO findByJobId(String jobId);

    /**
     * 保存作业依赖
     */
    JobDependencyBO save(String jobId, JobDependencyCmd cmd, String updatedBy);

    /**
     * 查询作业血缘
     */
    JobLineageBO lineage(String jobId);

    /**
     * 删除作业相关依赖
     */
    void deleteByJobId(String jobId);
}
