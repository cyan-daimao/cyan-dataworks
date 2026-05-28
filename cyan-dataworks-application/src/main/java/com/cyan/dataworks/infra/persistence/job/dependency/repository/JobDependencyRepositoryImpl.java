package com.cyan.dataworks.infra.persistence.job.dependency.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataworks.domain.job.dependency.JobDependency;
import com.cyan.dataworks.domain.job.dependency.repository.JobDependencyRepository;
import com.cyan.dataworks.infra.persistence.job.dependency.convert.JobDependencyInfraConvert;
import com.cyan.dataworks.infra.persistence.job.dependency.dos.JobDependencyDO;
import com.cyan.dataworks.infra.persistence.job.dependency.mappers.JobDependencyMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 作业依赖仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class JobDependencyRepositoryImpl implements JobDependencyRepository {

    /**
     * 作业依赖 Mapper
     */
    private final JobDependencyMapper jobDependencyMapper;

    public JobDependencyRepositoryImpl(JobDependencyMapper jobDependencyMapper) {
        this.jobDependencyMapper = jobDependencyMapper;
    }

    /**
     * 根据下游作业ID查询依赖
     */
    @Override
    public List<JobDependency> listByDownstreamJobId(String downstreamJobId) {
        LambdaQueryWrapper<JobDependencyDO> wrapper = new LambdaQueryWrapper<JobDependencyDO>()
                .eq(JobDependencyDO::getDownstreamJobId, Convert.toLong(downstreamJobId))
                .orderByAsc(JobDependencyDO::getCreatedAt);
        return toDomains(jobDependencyMapper.selectList(wrapper));
    }

    /**
     * 根据上游作业ID查询依赖
     */
    @Override
    public List<JobDependency> listByUpstreamJobId(String upstreamJobId) {
        LambdaQueryWrapper<JobDependencyDO> wrapper = new LambdaQueryWrapper<JobDependencyDO>()
                .eq(JobDependencyDO::getUpstreamJobId, Convert.toLong(upstreamJobId))
                .orderByAsc(JobDependencyDO::getCreatedAt);
        return toDomains(jobDependencyMapper.selectList(wrapper));
    }

    /**
     * 查询全部依赖
     */
    @Override
    public List<JobDependency> listAll() {
        LambdaQueryWrapper<JobDependencyDO> wrapper = new LambdaQueryWrapper<JobDependencyDO>()
                .orderByAsc(JobDependencyDO::getCreatedAt);
        return toDomains(jobDependencyMapper.selectList(wrapper));
    }

    /**
     * 保存依赖
     */
    @Override
    public JobDependency save(JobDependency dependency) {
        JobDependencyDO dependencyDO = JobDependencyInfraConvert.INSTANCE.toJobDependencyDO(dependency);
        jobDependencyMapper.insert(dependencyDO);
        return JobDependencyInfraConvert.INSTANCE.toJobDependency(jobDependencyMapper.selectById(dependencyDO.getId()));
    }

    /**
     * 替换下游作业的全部上游依赖
     */
    @Override
    public void replaceByDownstreamJobId(String downstreamJobId, List<JobDependency> dependencies) {
        jobDependencyMapper.delete(new LambdaQueryWrapper<JobDependencyDO>()
                .eq(JobDependencyDO::getDownstreamJobId, Convert.toLong(downstreamJobId)));
        Optional.ofNullable(dependencies).orElse(List.of())
                .forEach(this::save);
    }

    /**
     * 删除作业相关依赖
     */
    @Override
    public void deleteByJobId(String jobId) {
        Long id = Convert.toLong(jobId);
        jobDependencyMapper.delete(new LambdaQueryWrapper<JobDependencyDO>()
                .eq(JobDependencyDO::getUpstreamJobId, id)
                .or()
                .eq(JobDependencyDO::getDownstreamJobId, id));
    }

    /**
     * 转换领域对象列表
     */
    private List<JobDependency> toDomains(List<JobDependencyDO> dependencyDOs) {
        return Optional.ofNullable(dependencyDOs).orElse(List.of())
                .stream()
                .map(JobDependencyInfraConvert.INSTANCE::toJobDependency)
                .toList();
    }
}
