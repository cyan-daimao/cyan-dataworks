package com.cyan.dataworks.domain.workflow.repository;

import com.cyan.dataworks.domain.workflow.WorkflowEdge;

import java.util.List;

/**
 * 工作流依赖边仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowEdgeRepository {

    /**
     * 根据工作流ID查询依赖边
     */
    List<WorkflowEdge> listByWorkflowId(String workflowId);

    /**
     * 替换工作流依赖边
     */
    List<WorkflowEdge> replaceByWorkflowId(String workflowId, List<WorkflowEdge> edges);
}
