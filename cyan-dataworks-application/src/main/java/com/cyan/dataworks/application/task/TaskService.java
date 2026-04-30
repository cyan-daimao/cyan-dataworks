package com.cyan.dataworks.application.task;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.application.task.bo.DataWorkTaskBO;
import com.cyan.dataworks.application.task.cmd.DataWorkTaskCmd;
import com.cyan.dataworks.domain.task.query.DataWorkTaskPageQuery;

import java.util.List;

/**
 * 数据加工任务应用服务接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TaskService {

    /**
     * 分页查询任务
     */
    Page<DataWorkTaskBO> page(DataWorkTaskPageQuery query);

    /**
     * 列表查询任务
     */
    List<DataWorkTaskBO> list(DataWorkTaskPageQuery query);

    /**
     * 根据ID查询任务
     */
    DataWorkTaskBO findById(String id);

    /**
     * 保存任务
     */
    DataWorkTaskBO save(DataWorkTaskCmd cmd, String createdBy);

    /**
     * 更新任务
     */
    DataWorkTaskBO update(String id, DataWorkTaskCmd cmd);

    /**
     * 删除任务
     */
    void delete(String id);
}
