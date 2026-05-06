package com.cyan.dataworks.application.job;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.domain.job.query.JobPageQuery;

import java.util.List;

/**
 * 数据加工作业应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobService {

    /**
     * 分页查询作业
     */
    Page<JobBO> page(JobPageQuery query);

    /**
     * 列表查询作业
     */
    List<JobBO> list(JobPageQuery query);

    /**
     * 根据ID查询作业
     */
    JobBO findById(String id);

    /**
     * 保存作业
     */
    JobBO save(JobCmd cmd, String createdBy);

    /**
     * 更新作业
     */
    JobBO update(String id, JobCmd cmd, String updatedBy);

    /**
     * 删除作业
     */
    void delete(String id);
}
