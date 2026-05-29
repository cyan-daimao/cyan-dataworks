package com.cyan.dataworks.domain.workflow.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.workflow.Workflow;
import com.cyan.dataworks.domain.workflow.query.WorkflowPageQuery;

import java.util.List;

/**
 * 工作流仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowRepository {

    /**
     * 分页查询工作流
     */
    Page<Workflow> page(WorkflowPageQuery query);

    /**
     * 根据ID查询工作流
     */
    Workflow findById(String id);

    /**
     * 根据DAG ID查询工作流
     */
    Workflow findByDagId(String dagId);

    /**
     * 查询Airflow工作流
     */
    List<Workflow> listAirflowWorkflows();

    /**
     * 保存工作流
     */
    Workflow save(Workflow workflow);

    /**
     * 更新工作流
     */
    Workflow updateById(Workflow workflow);

    /**
     * 删除工作流
     */
    void deleteById(String id);
}
