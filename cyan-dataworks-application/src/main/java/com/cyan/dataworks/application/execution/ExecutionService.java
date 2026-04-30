package com.cyan.dataworks.application.execution;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.application.execution.bo.ExecutionRecordBO;
import com.cyan.dataworks.domain.execution.query.ExecutionRecordPageQuery;

/**
 * 执行记录应用服务接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface ExecutionService {

    /**
     * 手动执行任务
     */
    ExecutionRecordBO execute(String taskId);

    /**
     * 分页查询执行记录
     */
    Page<ExecutionRecordBO> page(ExecutionRecordPageQuery query);

    /**
     * 根据ID查询执行记录
     */
    ExecutionRecordBO findById(String id);
}
