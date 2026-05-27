package com.cyan.dataworks.infra.persistence.job.schedule.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.infra.persistence.job.schedule.convert.JobScheduleInfraConvert;
import com.cyan.dataworks.infra.persistence.job.schedule.dos.JobScheduleDO;
import com.cyan.dataworks.infra.persistence.job.schedule.mappers.JobScheduleMapper;
import org.springframework.stereotype.Repository;

/**
 * 作业调度配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class JobScheduleRepositoryImpl implements JobScheduleRepository {

    private final JobScheduleMapper jobScheduleMapper;

    public JobScheduleRepositoryImpl(JobScheduleMapper jobScheduleMapper) {
        this.jobScheduleMapper = jobScheduleMapper;
    }

    /**
     * 根据作业ID查询调度配置
     */
    @Override
    public JobSchedule findByJobId(String jobId) {
        LambdaQueryWrapper<JobScheduleDO> wrapper = new LambdaQueryWrapper<JobScheduleDO>()
                .eq(JobScheduleDO::getJobId, com.cyan.arch.common.util.Convert.toLong(jobId));
        JobScheduleDO jobScheduleDO = jobScheduleMapper.selectOne(wrapper);
        if (jobScheduleDO == null) {
            return null;
        }
        return JobScheduleInfraConvert.INSTANCE.toJobSchedule(jobScheduleDO);
    }

    /**
     * 保存调度配置
     */
    @Override
    public JobSchedule save(JobSchedule jobSchedule) {
        JobScheduleDO jobScheduleDO = JobScheduleInfraConvert.INSTANCE.toJobScheduleDO(jobSchedule);
        jobScheduleMapper.insert(jobScheduleDO);
        return findByJobId(jobSchedule.getJobId());
    }

    /**
     * 更新调度配置
     */
    @Override
    public JobSchedule updateById(JobSchedule jobSchedule) {
        JobScheduleDO jobScheduleDO = JobScheduleInfraConvert.INSTANCE.toJobScheduleDO(jobSchedule);
        jobScheduleMapper.updateById(jobScheduleDO);
        return findByJobId(jobSchedule.getJobId());
    }

    /**
     * 根据作业ID删除调度配置
     */
    @Override
    public void deleteByJobId(String jobId) {
        LambdaQueryWrapper<JobScheduleDO> wrapper = new LambdaQueryWrapper<JobScheduleDO>()
                .eq(JobScheduleDO::getJobId, com.cyan.arch.common.util.Convert.toLong(jobId));
        jobScheduleMapper.delete(wrapper);
    }

    /**
     * 查询已启用的调度配置
     */
    @Override
    public java.util.List<JobSchedule> listEnabled() {
        LambdaQueryWrapper<JobScheduleDO> wrapper = new LambdaQueryWrapper<JobScheduleDO>()
                .eq(JobScheduleDO::getEnabled, true)
                .eq(JobScheduleDO::getSchedulerType, SchedulerType.AIRFLOW);
        return java.util.Optional.ofNullable(jobScheduleMapper.selectList(wrapper)).orElse(java.util.List.of())
                .stream().map(JobScheduleInfraConvert.INSTANCE::toJobSchedule).toList();
    }
}
