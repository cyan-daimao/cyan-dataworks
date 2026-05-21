package com.cyan.dataworks.application.task.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.task.TaskFolderService;
import com.cyan.dataworks.domain.task.folder.TaskFolder;
import com.cyan.dataworks.domain.task.folder.TaskFolderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务文件夹应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class TaskFolderServiceImpl implements TaskFolderService {

    private final TaskFolderRepository taskFolderRepository;

    public TaskFolderServiceImpl(TaskFolderRepository taskFolderRepository) {
        this.taskFolderRepository = taskFolderRepository;
    }

    @Override
    public List<TaskFolder> findAll() {
        return taskFolderRepository.findAll();
    }

    @Override
    public TaskFolder save(String name, String parentId) {
        TaskFolder folder = new TaskFolder()
                .setName(name)
                .setParentId(parentId == null ? "0" : parentId);
        return folder.save(taskFolderRepository);
    }

    @Override
    public TaskFolder update(String id, String name, String parentId) {
        TaskFolder existing = taskFolderRepository.findById(id);
        Assert.notNull(existing, new SilentException("文件夹不存在"));
        TaskFolder folder = new TaskFolder()
                .setId(id)
                .setName(name)
                .setParentId(parentId);
        return folder.update(taskFolderRepository);
    }

    @Override
    public void delete(String id) {
        TaskFolder existing = taskFolderRepository.findById(id);
        Assert.notNull(existing, new SilentException("文件夹不存在"));
        existing.delete(taskFolderRepository);
    }
}
