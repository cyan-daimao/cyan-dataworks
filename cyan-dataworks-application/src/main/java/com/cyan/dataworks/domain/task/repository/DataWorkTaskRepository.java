package com.cyan.dataworks.domain.task.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.task.DataWorkTask;
import com.cyan.dataworks.domain.task.query.DataWorkTaskPageQuery;

import java.util.List;

/**
 * 数据加工任务仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface DataWorkTaskRepository {

    /**
     * 分页查询任务
     */
    Page<DataWorkTask> page(DataWorkTaskPageQuery query);

    /**
     * 列表查询任务
     */
    List<DataWorkTask> list(DataWorkTaskPageQuery query);

    /**
     * 根据ID查询任务
     */
    DataWorkTask findById(String id);

    /**
     * 保存任务
     */
    DataWorkTask save(DataWorkTask task);

    /**
     * 更新任务
     */
    DataWorkTask updateById(DataWorkTask task);

    /**
     * 删除任务
     */
    void deleteById(String id);
}
