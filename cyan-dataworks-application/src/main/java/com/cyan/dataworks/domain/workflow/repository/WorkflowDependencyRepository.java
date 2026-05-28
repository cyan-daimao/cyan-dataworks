package com.cyan.dataworks.domain.workflow.repository;

import com.cyan.dataworks.domain.workflow.WorkflowDependency;

import java.util.List;

/**
 * 工作流级依赖仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowDependencyRepository {

    /** 查询工作流上游依赖 */
    List<WorkflowDependency> listByDownstreamWorkflowId(String downstreamWorkflowId);

    /** 查询工作流下游依赖 */
    List<WorkflowDependency> listByUpstreamWorkflowId(String upstreamWorkflowId);

    /** 查询全部工作流依赖 */
    List<WorkflowDependency> listAll();

    /** 替换工作流上游依赖 */
    List<WorkflowDependency> replaceByDownstreamWorkflowId(String downstreamWorkflowId, List<WorkflowDependency> dependencies);

    /** 删除工作流相关依赖 */
    void deleteByWorkflowId(String workflowId);
}
