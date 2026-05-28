package com.cyan.dataworks.application.workflow;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.application.workflow.bo.WorkflowBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDagDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowScheduleBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowDefinitionCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowScheduleCmd;
import com.cyan.dataworks.domain.workflow.query.WorkflowPageQuery;

import java.util.List;

/**
 * 工作流应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowService {

    /** 分页查询工作流 */
    Page<WorkflowBO> page(WorkflowPageQuery query);

    /** 查询工作流详情 */
    WorkflowBO findById(String id);

    /** 保存工作流 */
    WorkflowBO save(WorkflowCmd cmd, String createdBy);

    /** 更新工作流 */
    WorkflowBO update(String id, WorkflowCmd cmd, String updatedBy);

    /** 删除工作流 */
    void delete(String id);

    /** 查询工作流定义 */
    WorkflowDefinitionBO findDefinition(String workflowId);

    /** 保存工作流定义 */
    WorkflowDefinitionBO saveDefinition(String workflowId, WorkflowDefinitionCmd cmd, String updatedBy);

    /** 查询工作流调度配置 */
    WorkflowScheduleBO findSchedule(String workflowId);

    /** 保存工作流调度配置 */
    WorkflowScheduleBO saveSchedule(String workflowId, WorkflowScheduleCmd cmd, String updatedBy);

    /** 发布工作流 */
    WorkflowBO publish(String id, String updatedBy);

    /** 下线工作流 */
    WorkflowBO offline(String id, String updatedBy);

    /** 查询Airflow DAG定义 */
    List<WorkflowDagDefinitionBO> listAirflowDagDefinitions();

    /** 确保作业存在默认单节点工作流 */
    WorkflowBO ensureSingleNodeWorkflow(String jobId, String operator);
}
