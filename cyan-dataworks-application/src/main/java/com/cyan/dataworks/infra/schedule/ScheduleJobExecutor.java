package com.cyan.dataworks.infra.schedule;

import com.cyan.dataworks.application.execution.ExecutionService;
import com.cyan.dataworks.domain.schedule.ScheduleConfig;
import com.cyan.dataworks.domain.schedule.repository.ScheduleConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
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
    private final ScheduleConfigRepository scheduleConfigRepository;
    private final ExecutionService executionService;

    /**
     * 存储已注册的任务 Future，key 为 taskId
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public ScheduleJobExecutor(TaskScheduler taskScheduler,
                               ScheduleConfigRepository scheduleConfigRepository,
                               ExecutionService executionService) {
        this.taskScheduler = taskScheduler;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.executionService = executionService;
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
     * 注册或刷新指定任务的定时调度
     *
     * @param config 调度配置
     */
    public void registerOrUpdate(ScheduleConfig config) {
        if (config == null || config.getTaskId() == null) {
            return;
        }
        String taskId = config.getTaskId();
        // 先取消旧任务
        cancel(taskId);
        // 如果启用且 Cron 表达式有效，则注册新任务
        if (Boolean.TRUE.equals(config.getEnabled()) && config.getCronExpression() != null
                && !config.getCronExpression().isEmpty()) {
            try {
                ScheduledFuture<?> future = taskScheduler.schedule(
                        () -> executeTask(taskId),
                        new CronTrigger(config.getCronExpression())
                );
                scheduledTasks.put(taskId, future);
                log.info("任务 [{}] 定时调度已注册，Cron: {}", taskId, config.getCronExpression());
            } catch (Exception e) {
                log.error("任务 [{}] Cron 表达式无效: {}", taskId, config.getCronExpression(), e);
            }
        }
    }

    /**
     * 取消指定任务的定时调度
     *
     * @param taskId 任务ID
     */
    public void cancel(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            log.info("任务 [{}] 定时调度已取消", taskId);
        }
    }

    /**
     * 执行定时任务
     *
     * @param taskId 任务ID
     */
    private void executeTask(String taskId) {
        try {
            log.info("定时任务 [{}] 开始执行", taskId);
            executionService.execute(taskId);
            log.info("定时任务 [{}] 执行完成", taskId);
        } catch (Exception e) {
            log.error("定时任务 [{}] 执行失败", taskId, e);
        }
    }
}
