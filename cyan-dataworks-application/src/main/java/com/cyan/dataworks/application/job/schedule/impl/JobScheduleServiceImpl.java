package com.cyan.dataworks.application.job.schedule.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.schedule.JobScheduleService;
import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import com.cyan.dataworks.application.job.schedule.cmd.JobScheduleCmd;
import com.cyan.dataworks.application.workflow.WorkflowService;
import com.cyan.dataworks.application.workflow.bo.WorkflowBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowScheduleBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowScheduleCmd;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.enums.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 工作流应用服务 */
    private final WorkflowService workflowService;

    public JobScheduleServiceImpl(JobRepository jobRepository,
                                  WorkflowService workflowService) {
        this.jobRepository = jobRepository;
        this.workflowService = workflowService;
    }

    /**
     * 根据作业ID查询调度配置
     */
    @Override
    public JobScheduleBO findByJobId(String jobId) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        WorkflowBO workflow = workflowService.ensureSingleNodeWorkflow(jobId, "system");
        WorkflowScheduleBO schedule = workflowService.findSchedule(workflow.getId());
        if (schedule == null) {
            return null;
        }
        return toJobScheduleBO(jobId, schedule);
    }

    /**
     * 保存或更新调度配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobScheduleBO saveOrUpdate(String jobId, JobScheduleCmd cmd, String operator) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        WorkflowBO workflow = workflowService.ensureSingleNodeWorkflow(jobId, operator);
        WorkflowScheduleBO schedule = workflowService.saveSchedule(workflow.getId(), new WorkflowScheduleCmd()
                .setCronExpression(cmd.getCronExpression())
                .setEnabled(cmd.getEnabled())
                .setSchedulerType(cmd.getSchedulerType()), operator);
        if (job.getStatus() == TaskStatus.ONLINE) {
            workflowService.publish(workflow.getId(), operator);
        }
        return toJobScheduleBO(jobId, schedule);
    }

    private JobScheduleBO toJobScheduleBO(String jobId, WorkflowScheduleBO schedule) {
        return new JobScheduleBO()
                .setId(schedule.getId())
                .setJobId(jobId)
                .setCronExpression(schedule.getCronExpression())
                .setEnabled(schedule.getEnabled())
                .setSchedulerType(schedule.getSchedulerType())
                .setNextExecuteTime(schedule.getNextExecuteTime());
    }
}
