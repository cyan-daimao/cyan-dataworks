package com.cyan.dataworks.domain.job.dependency.repository;

import com.cyan.dataworks.domain.job.dependency.JobDependency;

import java.util.List;

/**
 * 作业依赖仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobDependencyRepository {

    /**
     * 根据下游作业ID查询依赖
     */
    List<JobDependency> listByDownstreamJobId(String downstreamJobId);

    /**
     * 根据上游作业ID查询依赖
     */
    List<JobDependency> listByUpstreamJobId(String upstreamJobId);

    /**
     * 查询全部依赖
     */
    List<JobDependency> listAll();

    /**
     * 保存依赖
     */
    JobDependency save(JobDependency dependency);

    /**
     * 替换下游作业的全部上游依赖
     */
    void replaceByDownstreamJobId(String downstreamJobId, List<JobDependency> dependencies);

    /**
     * 删除作业相关依赖
     */
    void deleteByJobId(String jobId);
}
