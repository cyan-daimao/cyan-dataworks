package com.cyan.dataworks.infra.persistence.job_instance.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.query.JobInstancePageQuery;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.infra.persistence.job_instance.convert.JobInstanceInfraConvert;
import com.cyan.dataworks.infra.persistence.job_instance.dos.JobInstanceDO;
import com.cyan.dataworks.infra.persistence.job_instance.mappers.JobInstanceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业实例仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class JobInstanceRepositoryImpl implements JobInstanceRepository {

    private final JobInstanceMapper jobInstanceMapper;

    public JobInstanceRepositoryImpl(JobInstanceMapper jobInstanceMapper) {
        this.jobInstanceMapper = jobInstanceMapper;
    }

    /**
     * 分页查询实例
     */
    @Override
    public Page<JobInstance> page(JobInstancePageQuery query) {
        LambdaQueryWrapper<JobInstanceDO> wrapper = new LambdaQueryWrapper<JobInstanceDO>()
                .eq(query.getJobId() != null, JobInstanceDO::getJobId, com.cyan.arch.common.util.Convert.toLong(query.getJobId()))
                .eq(query.getStatus() != null, JobInstanceDO::getStatus, query.getStatus())
                .orderByDesc(JobInstanceDO::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<JobInstanceDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = jobInstanceMapper.selectPage(page, wrapper);
        List<JobInstance> data = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(JobInstanceInfraConvert.INSTANCE::toJobInstance).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据ID查询实例
     */
    @Override
    public JobInstance findById(String id) {
        JobInstanceDO jobInstanceDO = jobInstanceMapper.selectById(id);
        if (jobInstanceDO == null) {
            return null;
        }
        return JobInstanceInfraConvert.INSTANCE.toJobInstance(jobInstanceDO);
    }

    /**
     * 保存实例
     */
    @Override
    public JobInstance save(JobInstance jobInstance) {
        JobInstanceDO jobInstanceDO = JobInstanceInfraConvert.INSTANCE.toJobInstanceDO(jobInstance);
        jobInstanceMapper.insert(jobInstanceDO);
        return findById(jobInstanceDO.getId() + "");
    }

    /**
     * 更新实例
     */
    @Override
    public JobInstance updateById(JobInstance jobInstance) {
        JobInstanceDO jobInstanceDO = JobInstanceInfraConvert.INSTANCE.toJobInstanceDO(jobInstance);
        jobInstanceMapper.updateById(jobInstanceDO);
        return findById(jobInstance.getId());
    }
}
