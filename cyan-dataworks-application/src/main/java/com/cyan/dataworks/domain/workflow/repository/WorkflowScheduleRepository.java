package com.cyan.dataworks.domain.workflow.repository;

import com.cyan.dataworks.domain.workflow.WorkflowSchedule;

import java.util.List;

/**
 * 工作流调度配置仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowScheduleRepository {

    /**
     * 根据工作流ID查询调度配置
     */
    WorkflowSchedule findByWorkflowId(String workflowId);

    /**
     * 查询Airflow调度配置
     */
    List<WorkflowSchedule> listAirflow();

    /**
     * 保存调度配置
     */
    WorkflowSchedule save(WorkflowSchedule schedule);

    /**
     * 更新调度配置
     */
    WorkflowSchedule updateById(WorkflowSchedule schedule);

    /**
     * 根据工作流ID删除调度配置
     */
    void deleteByWorkflowId(String workflowId);
}
