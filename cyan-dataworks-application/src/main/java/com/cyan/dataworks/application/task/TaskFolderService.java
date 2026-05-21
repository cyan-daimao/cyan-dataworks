package com.cyan.dataworks.application.task;

import com.cyan.dataworks.domain.task.folder.TaskFolder;

import java.util.List;

/**
 * 任务文件夹应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TaskFolderService {

    /**
     * 查询所有文件夹
     */
    List<TaskFolder> findAll();

    /**
     * 保存文件夹
     */
    TaskFolder save(String name, String parentId);

    /**
     * 更新文件夹
     */
    TaskFolder update(String id, String name, String parentId);

    /**
     * 删除文件夹
     */
    void delete(String id);
}
