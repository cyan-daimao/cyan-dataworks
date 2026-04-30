package com.cyan.dataworks.domain.task.folder;

import java.util.List;

/**
 * 任务文件夹仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TaskFolderRepository {

    /**
     * 保存文件夹
     */
    TaskFolder save(TaskFolder folder);

    /**
     * 根据ID更新文件夹
     */
    TaskFolder updateById(TaskFolder folder);

    /**
     * 根据ID查询文件夹
     */
    TaskFolder findById(String id);

    /**
     * 根据父ID查询子文件夹列表
     */
    List<TaskFolder> findByParentId(String parentId);

    /**
     * 查询所有文件夹
     */
    List<TaskFolder> findAll();

    /**
     * 根据ID删除文件夹
     */
    void deleteById(String id);
}
