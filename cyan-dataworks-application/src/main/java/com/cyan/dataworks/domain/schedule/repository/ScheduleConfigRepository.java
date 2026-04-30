package com.cyan.dataworks.domain.schedule.repository;

import com.cyan.dataworks.domain.schedule.ScheduleConfig;
import com.cyan.dataworks.domain.schedule.query.ScheduleConfigQuery;

/**
 * 调度配置仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface ScheduleConfigRepository {

    /**
     * 根据任务ID查询调度配置
     */
    ScheduleConfig findByTaskId(String taskId);

    /**
     * 保存调度配置
     */
    ScheduleConfig save(ScheduleConfig scheduleConfig);

    /**
     * 更新调度配置
     */
    ScheduleConfig updateById(ScheduleConfig scheduleConfig);

    /**
     * 根据任务ID删除调度配置
     */
    void deleteByTaskId(String taskId);
}
