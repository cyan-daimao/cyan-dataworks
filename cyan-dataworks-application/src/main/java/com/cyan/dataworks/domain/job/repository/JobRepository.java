package com.cyan.dataworks.domain.job.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.query.JobPageQuery;

import java.util.List;

/**
 * 数据加工作业仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobRepository {

    /**
     * 分页查询作业
     */
    Page<Job> page(JobPageQuery query);

    /**
     * 列表查询作业
     */
    List<Job> list(JobPageQuery query);

    /**
     * 根据ID查询作业
     */
    Job findById(String id);

    /**
     * 保存作业
     */
    Job save(Job job);

    /**
     * 更新作业
     */
    Job updateById(Job job);

    /**
     * 删除作业
     */
    void deleteById(String id);
}
