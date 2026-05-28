package com.cyan.dataworks.domain.workflow.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.workflow.WorkflowInstance;
import com.cyan.dataworks.domain.workflow.query.WorkflowInstancePageQuery;

/**
 * 工作流实例仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowInstanceRepository {

    /**
     * 分页查询工作流实例
     */
    Page<WorkflowInstance> page(WorkflowInstancePageQuery query);

    /**
     * 根据ID查询实例
     */
    WorkflowInstance findById(String id);

    /**
     * 根据DAG Run查询实例
     */
    WorkflowInstance findByDagRun(String dagId, String dagRunId);

    /**
     * 保存实例
     */
    WorkflowInstance save(WorkflowInstance instance);

    /**
     * 更新实例
     */
    WorkflowInstance updateById(WorkflowInstance instance);
}
