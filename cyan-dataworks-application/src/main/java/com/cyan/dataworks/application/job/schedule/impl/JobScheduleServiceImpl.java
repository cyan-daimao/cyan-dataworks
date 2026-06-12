package com.cyan.dataworks.application.job.schedule.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.schedule.JobScheduleService;
import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import com.cyan.dataworks.application.job.schedule.cmd.JobScheduleCmd;
import com.cyan.dataworks.application.job.schedule.convert.JobScheduleAppConvert;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.infra.remote.airflow.AirflowOrchestrationGateway;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.enums.TaskStatus;
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

    /** 作业仓储 */
    private final JobRepository jobRepository;

    /** 作业调度仓储 */
    private final JobScheduleRepository jobScheduleRepository;

    /** Airflow编排网关 */
    private final AirflowOrchestrationGateway airflowGateway;

    public JobScheduleServiceImpl(JobRepository jobRepository,
                                  JobScheduleRepository jobScheduleRepository,
                                  AirflowOrchestrationGateway airflowGateway) {
        this.jobRepository = jobRepository;
        this.jobScheduleRepository = jobScheduleRepository;
        this.airflowGateway = airflowGateway;
    }

    /**
     * 根据作业ID查询调度配置
     */
    @Override
    public JobScheduleBO findByJobId(String jobId) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        JobSchedule schedule = jobScheduleRepository.findByJobId(jobId);
        if (schedule == null) {
            return null;
        }
        return JobScheduleAppConvert.INSTANCE.toJobScheduleBO(schedule);
    }

    /**
     * 保存或更新调度配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobScheduleBO saveOrUpdate(String jobId, JobScheduleCmd cmd, String operator) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        JobSchedule schedule = JobScheduleAppConvert.INSTANCE.toJobSchedule(cmd)
                .setJobId(jobId)
                .setUpdatedBy(operator);
        validateScheduleSupported(job, schedule);
        validateCronExpressionIfEnabled(schedule);
        JobSchedule existing = jobScheduleRepository.findByJobId(jobId);
        if (existing == null) {
            schedule.setCreatedBy(operator);
            schedule = schedule.save(jobScheduleRepository);
        } else {
            schedule.setId(existing.getId())
                    .setCreatedBy(existing.getCreatedBy())
                    .setCreatedAt(existing.getCreatedAt());
            schedule = schedule.update(jobScheduleRepository);
        }
        if (job.getStatus() == TaskStatus.ONLINE && job.getNodeType() != NodeType.FLINK_SQL) {
            airflowGateway.syncDagPaused(airflowGateway.buildJobDagId(jobId), !Boolean.TRUE.equals(schedule.getEnabled()), false);
        }
        return JobScheduleAppConvert.INSTANCE.toJobScheduleBO(schedule);
    }

    private void validateCronExpressionIfEnabled(JobSchedule schedule) {
        if (Boolean.TRUE.equals(schedule.getEnabled())) {
            validateCronExpression(schedule.getCronExpression());
        }
    }

    private void validateScheduleSupported(Job job, JobSchedule schedule) {
        if (job.getNodeType() == NodeType.FLINK_SQL && Boolean.TRUE.equals(schedule.getEnabled())) {
            throw new SilentException("FlinkSQL实时任务不支持Airflow调度，请使用FlinkSQL批任务");
        }
    }

    private void validateCronExpression(String cronExpression) {
        Assert.isTrue(isValidCronExpression(cronExpression), new SilentException("Cron表达式不合法，请检查格式: " + cronExpression));
    }

    private boolean isValidCronExpression(String cronExpression) {
        String normalizedCron = normalizeToAirflowCron(cronExpression);
        if (normalizedCron == null || normalizedCron.isBlank()) {
            return false;
        }
        try {
            CronExpression.parse("0 " + normalizedCron);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

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
