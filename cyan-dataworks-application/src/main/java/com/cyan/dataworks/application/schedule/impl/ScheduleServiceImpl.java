package com.cyan.dataworks.application.schedule.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.schedule.ScheduleService;
import com.cyan.dataworks.application.schedule.bo.ScheduleConfigBO;
import com.cyan.dataworks.application.schedule.cmd.ScheduleConfigCmd;
import com.cyan.dataworks.application.schedule.convert.ScheduleAppConvert;
import com.cyan.dataworks.domain.schedule.ScheduleConfig;
import com.cyan.dataworks.domain.schedule.repository.ScheduleConfigRepository;
import com.cyan.dataworks.domain.task.DataWorkTask;
import com.cyan.dataworks.domain.task.repository.DataWorkTaskRepository;
import com.cyan.dataworks.infra.schedule.ScheduleJobExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度配置应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleConfigRepository scheduleConfigRepository;
    private final DataWorkTaskRepository dataWorkTaskRepository;
    private final ScheduleJobExecutor scheduleJobExecutor;

    public ScheduleServiceImpl(ScheduleConfigRepository scheduleConfigRepository,
                               DataWorkTaskRepository dataWorkTaskRepository,
                               ScheduleJobExecutor scheduleJobExecutor) {
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.dataWorkTaskRepository = dataWorkTaskRepository;
        this.scheduleJobExecutor = scheduleJobExecutor;
    }

    /**
     * 根据任务ID查询调度配置
     */
    @Override
    public ScheduleConfigBO findByTaskId(String taskId) {
        ScheduleConfig scheduleConfig = scheduleConfigRepository.findByTaskId(taskId);
        if (scheduleConfig == null) {
            return null;
        }
        return ScheduleAppConvert.INSTANCE.toScheduleConfigBO(scheduleConfig);
    }

    /**
     * 保存或更新调度配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleConfigBO saveOrUpdate(String taskId, ScheduleConfigCmd cmd) {
        DataWorkTask task = dataWorkTaskRepository.findById(taskId);
        Assert.notNull(task, new SilentException("任务不存在"));

        ScheduleConfig existing = scheduleConfigRepository.findByTaskId(taskId);
        ScheduleConfig scheduleConfig = ScheduleAppConvert.INSTANCE.toScheduleConfig(cmd);
        scheduleConfig.setTaskId(taskId);

        ScheduleConfig result;
        if (existing == null) {
            result = scheduleConfig.save(scheduleConfigRepository);
        } else {
            scheduleConfig.setId(existing.getId());
            result = scheduleConfig.update(scheduleConfigRepository);
        }

        // 动态注册或刷新定时任务
        scheduleJobExecutor.registerOrUpdate(result);

        return ScheduleAppConvert.INSTANCE.toScheduleConfigBO(result);
    }
}
