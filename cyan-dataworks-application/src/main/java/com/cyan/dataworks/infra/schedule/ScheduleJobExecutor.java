package com.cyan.dataworks.infra.schedule;

import com.cyan.dataworks.application.job_instance.JobInstanceService;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时调度任务执行器
 * <p>
 * 基于 Spring TaskScheduler 动态管理 Cron 任务，
 * 在应用启动时加载所有启用的调度配置，并在配置变更时动态刷新。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class ScheduleJobExecutor {

    private final TaskScheduler taskScheduler;
    private final JobScheduleRepository jobScheduleRepository;
    private final JobInstanceService jobInstanceService;

    /**
     * 存储已注册的任务 Future，key 为 jobId
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public ScheduleJobExecutor(TaskScheduler taskScheduler,
                               JobScheduleRepository jobScheduleRepository,
                               JobInstanceService jobInstanceService) {
        this.taskScheduler = taskScheduler;
        this.jobScheduleRepository = jobScheduleRepository;
        this.jobInstanceService = jobInstanceService;
    }

    /**
     * 应用启动时加载所有启用的调度任务
     */
    @PostConstruct
    public void loadAllSchedules() {
        log.info("开始加载定时调度任务...");
        // 当前实现：启动时无全量扫描方法，可通过扩展 repository.listEnabled() 加载
        // 一期简化：由配置变更时动态触发注册
    }

    /**
     * 注册或刷新指定作业的定时调度
     *
     * @param config 调度配置
     */
    public void registerOrUpdate(JobSchedule config) {
        if (config == null || config.getJobId() == null) {
            return;
        }
        String jobId = config.getJobId();
        // 先取消旧任务
        cancel(jobId);
        // 如果启用且 Cron 表达式有效，则注册新任务
        if (Boolean.TRUE.equals(config.getEnabled()) && config.getCronExpression() != null
                && !config.getCronExpression().isEmpty()) {
            try {
                ScheduledFuture<?> future = taskScheduler.schedule(
                        () -> executeJob(jobId),
                        new CronTrigger(config.getCronExpression())
                );
                scheduledTasks.put(jobId, future);
                log.info("作业 [{}] 定时调度已注册，Cron: {}", jobId, config.getCronExpression());
            } catch (Exception e) {
                log.error("作业 [{}] Cron 表达式无效: {}", jobId, config.getCronExpression(), e);
            }
        }
    }

    /**
     * 取消指定作业的定时调度
     *
     * @param jobId 作业ID
     */
    public void cancel(String jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            log.info("作业 [{}] 定时调度已取消", jobId);
        }
    }

    /**
     * 执行定时作业
     *
     * @param jobId 作业ID
     */
    private void executeJob(String jobId) {
        try {
            log.info("定时作业 [{}] 开始执行", jobId);
            jobInstanceService.execute(jobId);
            log.info("定时作业 [{}] 执行完成", jobId);
        } catch (Exception e) {
            log.error("定时作业 [{}] 执行失败", jobId, e);
        }
    }
}
