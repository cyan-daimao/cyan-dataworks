package com.cyan.dataworks.domain.workflow.repository;

import com.cyan.dataworks.domain.workflow.WorkflowNode;

import java.util.List;

/**
 * 工作流节点仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowNodeRepository {

    /**
     * 根据工作流ID查询节点
     */
    List<WorkflowNode> listByWorkflowId(String workflowId);

    /**
     * 根据ID查询节点
     */
    WorkflowNode findById(String id);

    /**
     * 替换工作流节点
     */
    List<WorkflowNode> replaceByWorkflowId(String workflowId, List<WorkflowNode> nodes);
}
