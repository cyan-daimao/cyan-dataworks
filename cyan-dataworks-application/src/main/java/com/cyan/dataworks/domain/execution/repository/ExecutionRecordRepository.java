package com.cyan.dataworks.domain.execution.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.execution.ExecutionRecord;
import com.cyan.dataworks.domain.execution.query.ExecutionRecordPageQuery;

/**
 * 执行记录仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface ExecutionRecordRepository {

    /**
     * 分页查询执行记录
     */
    Page<ExecutionRecord> page(ExecutionRecordPageQuery query);

    /**
     * 根据ID查询执行记录
     */
    ExecutionRecord findById(String id);

    /**
     * 保存执行记录
     */
    ExecutionRecord save(ExecutionRecord executionRecord);

    /**
     * 更新执行记录
     */
    ExecutionRecord updateById(ExecutionRecord executionRecord);
}
