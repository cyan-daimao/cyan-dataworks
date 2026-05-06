package com.cyan.dataworks.infra.persistence.job.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.query.JobPageQuery;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.infra.persistence.job.convert.JobInfraConvert;
import com.cyan.dataworks.infra.persistence.job.dos.JobDO;
import com.cyan.dataworks.infra.persistence.job.mappers.JobMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class JobRepositoryImpl implements JobRepository {

    private final JobMapper jobMapper;

    public JobRepositoryImpl(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    /**
     * 分页查询作业
     */
    @Override
    public Page<Job> page(JobPageQuery query) {
        LambdaQueryWrapper<JobDO> wrapper = new LambdaQueryWrapper<JobDO>()
                .like(StrUtils.isNotBlank(query.getName()), JobDO::getName, query.getName())
                .eq(query.getEngineType() != null, JobDO::getEngineType, query.getEngineType())
                .eq(StrUtils.isNotBlank(query.getCreatedBy()), JobDO::getCreatedBy, query.getCreatedBy())
                .eq(query.getFolderId() != null, JobDO::getFolderId, query.getFolderId())
                .orderByDesc(JobDO::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<JobDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = jobMapper.selectPage(page, wrapper);
        List<Job> data = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(JobInfraConvert.INSTANCE::toJob).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 列表查询作业
     */
    @Override
    public List<Job> list(JobPageQuery query) {
        LambdaQueryWrapper<JobDO> wrapper = new LambdaQueryWrapper<JobDO>()
                .like(StrUtils.isNotBlank(query.getName()), JobDO::getName, query.getName())
                .eq(query.getEngineType() != null, JobDO::getEngineType, query.getEngineType())
                .eq(StrUtils.isNotBlank(query.getCreatedBy()), JobDO::getCreatedBy, query.getCreatedBy())
                .eq(query.getFolderId() != null, JobDO::getFolderId, query.getFolderId())
                .orderByDesc(JobDO::getCreatedAt);
        List<JobDO> dos = jobMapper.selectList(wrapper);
        return Optional.ofNullable(dos).orElse(List.of())
                .stream().map(JobInfraConvert.INSTANCE::toJob).toList();
    }

    /**
     * 根据ID查询作业
     */
    @Override
    public Job findById(String id) {
        JobDO jobDO = jobMapper.selectById(id);
        if (jobDO == null) {
            return null;
        }
        return JobInfraConvert.INSTANCE.toJob(jobDO);
    }

    /**
     * 保存作业
     */
    @Override
    public Job save(Job job) {
        JobDO jobDO = JobInfraConvert.INSTANCE.toJobDO(job);
        jobMapper.insert(jobDO);
        return findById(jobDO.getId() + "");
    }

    /**
     * 更新作业
     */
    @Override
    public Job updateById(Job job) {
        JobDO jobDO = JobInfraConvert.INSTANCE.toJobDO(job);
        jobMapper.updateById(jobDO);
        return findById(job.getId());
    }

    /**
     * 删除作业
     */
    @Override
    public void deleteById(String id) {
        jobMapper.deleteById(id);
    }
}
