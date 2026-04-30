package com.cyan.dataworks.application.schedule;

import com.cyan.dataworks.application.schedule.bo.ScheduleConfigBO;
import com.cyan.dataworks.application.schedule.cmd.ScheduleConfigCmd;

/**
 * 调度配置应用服务接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface ScheduleService {

    /**
     * 根据任务ID查询调度配置
     */
    ScheduleConfigBO findByTaskId(String taskId);

    /**
     * 保存或更新调度配置
     */
    ScheduleConfigBO saveOrUpdate(String taskId, ScheduleConfigCmd cmd);
}
