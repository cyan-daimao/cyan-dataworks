package com.cyan.dataworks.application.job.schedule.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.schedule.JobScheduleService;
import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import com.cyan.dataworks.application.job.schedule.cmd.JobScheduleCmd;
import com.cyan.dataworks.application.job.schedule.convert.JobScheduleAppConvert;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.dataworks.infra.remote.airflow.AirflowDagStateService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 作业调度配置应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class JobScheduleServiceImpl implements JobScheduleService {

    private final JobScheduleRepository jobScheduleRepository;
    private final JobRepository jobRepository;
    private final AirflowDagStateService airflowDagStateService;

    public JobScheduleServiceImpl(JobScheduleRepository jobScheduleRepository,
                                  JobRepository jobRepository,
                                  AirflowDagStateService airflowDagStateService) {
        this.jobScheduleRepository = jobScheduleRepository;
        this.jobRepository = jobRepository;
        this.airflowDagStateService = airflowDagStateService;
    }

    /**
     * 根据作业ID查询调度配置
     */
    @Override
    public JobScheduleBO findByJobId(String jobId) {
        JobSchedule jobSchedule = jobScheduleRepository.findByJobId(jobId);
        if (jobSchedule == null) {
            return null;
        }
        return JobScheduleAppConvert.INSTANCE.toJobScheduleBO(jobSchedule);
    }

    /**
     * 保存或更新调度配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobScheduleBO saveOrUpdate(String jobId, JobScheduleCmd cmd) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));

        JobSchedule existing = jobScheduleRepository.findByJobId(jobId);
        JobSchedule jobSchedule = JobScheduleAppConvert.INSTANCE.toJobSchedule(cmd);
        jobSchedule.setJobId(jobId);
        validateCronExpressionIfEnabled(jobSchedule);

        JobSchedule result;
        if (existing == null) {
            result = jobSchedule.save(jobScheduleRepository);
        } else {
            jobSchedule.setId(existing.getId());
            result = jobSchedule.update(jobScheduleRepository);
        }
        syncAirflowDagStateIfNeeded(job, result);

        return JobScheduleAppConvert.INSTANCE.toJobScheduleBO(result);
    }

    /**
     * 同步Airflow DAG暂停状态
     */
    private void syncAirflowDagStateIfNeeded(Job job, JobSchedule jobSchedule) {
        if (job.getStatus() != TaskStatus.ONLINE) {
            return;
        }
        if (jobSchedule.getSchedulerType() != SchedulerType.AIRFLOW) {
            return;
        }
        airflowDagStateService.syncJobDagPaused(job, !Boolean.TRUE.equals(jobSchedule.getEnabled()), true);
    }

    /**
     * 启用调度时校验Cron表达式
     */
    private void validateCronExpressionIfEnabled(JobSchedule jobSchedule) {
        if (!Boolean.TRUE.equals(jobSchedule.getEnabled())) {
            return;
        }
        String normalizedCron = normalizeToAirflowCron(jobSchedule.getCronExpression());
        Assert.notBlank(normalizedCron, new SilentException("Cron表达式不合法"));
        try {
            CronExpression.parse("0 " + normalizedCron);
        } catch (IllegalArgumentException e) {
            throw new SilentException("Cron表达式不合法，请检查格式: " + jobSchedule.getCronExpression());
        }
    }

    /**
     * 归一化为Airflow 5段Cron表达式
     */
    private String normalizeToAirflowCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return null;
        }
        List<String> rawParts = Arrays.stream(cronExpression.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .toList();
        if (rawParts.isEmpty()) {
            return null;
        }
        List<String> parts = rawParts.stream()
                .map(part -> part.replace("?", "*").replace("？", "*"))
                .toList();
        if (parts.size() == 5) {
            if (rawParts.get(4).endsWith("?") || rawParts.get(4).endsWith("？")) {
                return String.join(" ", parts.get(1), parts.get(2), parts.get(3), "*", "*");
            }
            return String.join(" ", parts);
        }
        if (parts.size() == 6 || parts.size() == 7) {
            return String.join(" ", parts.subList(1, 6));
        }
        return null;
    }
}
