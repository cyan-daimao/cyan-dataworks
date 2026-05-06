package com.cyan.dataworks.application.job.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.JobService;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.application.job.convert.JobAppConvert;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.query.JobPageQuery;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.schedule.repository.ScheduleConfigRepository;
import com.cyan.dataworks.infra.schedule.ScheduleJobExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ScheduleConfigRepository scheduleConfigRepository;
    private final ScheduleJobExecutor scheduleJobExecutor;

    public JobServiceImpl(JobRepository jobRepository,
                          ScheduleConfigRepository scheduleConfigRepository,
                          ScheduleJobExecutor scheduleJobExecutor) {
        this.jobRepository = jobRepository;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.scheduleJobExecutor = scheduleJobExecutor;
    }

    /**
     * 分页查询作业
     */
    @Override
    public Page<JobBO> page(JobPageQuery query) {
        Page<Job> page = jobRepository.page(query);
        List<JobBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(JobAppConvert.INSTANCE::toJobBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 列表查询作业
     */
    @Override
    public List<JobBO> list(JobPageQuery query) {
        List<Job> jobs = jobRepository.list(query);
        return Optional.ofNullable(jobs).orElse(List.of())
                .stream().map(JobAppConvert.INSTANCE::toJobBO).toList();
    }

    /**
     * 根据ID查询作业
     */
    @Override
    public JobBO findById(String id) {
        Job job = jobRepository.findById(id);
        Assert.notNull(job, new SilentException("作业不存在"));
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * 保存作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobBO save(JobCmd cmd, String createdBy) {
        Job job = JobAppConvert.INSTANCE.toJob(cmd);
        job.setCreatedBy(createdBy);
        job = job.save(jobRepository);
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * 更新作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobBO update(String id, JobCmd cmd) {
        Job existing = jobRepository.findById(id);
        Assert.notNull(existing, new SilentException("作业不存在"));
        Job job = JobAppConvert.INSTANCE.toJob(cmd);
        job.setId(id);
        job = job.update(jobRepository);
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * 删除作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Job existing = jobRepository.findById(id);
        Assert.notNull(existing, new SilentException("作业不存在"));
        existing.delete(jobRepository);
        scheduleConfigRepository.deleteByTaskId(id);
        scheduleJobExecutor.cancel(id);
    }
}
