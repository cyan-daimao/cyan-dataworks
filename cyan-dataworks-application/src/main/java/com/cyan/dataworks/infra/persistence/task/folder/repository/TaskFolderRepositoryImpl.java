package com.cyan.dataworks.infra.persistence.task.folder.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyan.dataworks.domain.task.folder.TaskFolder;
import com.cyan.dataworks.domain.task.folder.TaskFolderRepository;
import com.cyan.dataworks.infra.persistence.task.folder.convert.TaskFolderInfraConvert;
import com.cyan.dataworks.infra.persistence.task.folder.dos.TaskFolderDO;
import com.cyan.dataworks.infra.persistence.task.folder.mappers.TaskFolderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 任务文件夹仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class TaskFolderRepositoryImpl implements TaskFolderRepository {

    private final TaskFolderMapper taskFolderMapper;

    public TaskFolderRepositoryImpl(TaskFolderMapper taskFolderMapper) {
        this.taskFolderMapper = taskFolderMapper;
    }

    @Override
    public TaskFolder save(TaskFolder folder) {
        TaskFolderDO taskFolderDO = TaskFolderInfraConvert.INSTANCE.toTaskFolderDO(folder);
        taskFolderMapper.insert(taskFolderDO);
        return findById(taskFolderDO.getId() + "");
    }

    @Override
    public TaskFolder updateById(TaskFolder folder) {
        TaskFolderDO taskFolderDO = TaskFolderInfraConvert.INSTANCE.toTaskFolderDO(folder);
        taskFolderMapper.updateById(taskFolderDO);
        return findById(folder.getId());
    }

    @Override
    public TaskFolder findById(String id) {
        TaskFolderDO taskFolderDO = taskFolderMapper.selectById(id);
        if (taskFolderDO == null) {
            return null;
        }
        return TaskFolderInfraConvert.INSTANCE.toTaskFolder(taskFolderDO);
    }

    @Override
    public List<TaskFolder> findByParentId(String parentId) {
        QueryWrapper<TaskFolderDO> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId == null ? 0L : Long.valueOf(parentId));
        List<TaskFolderDO> dos = taskFolderMapper.selectList(wrapper);
        return Optional.ofNullable(dos).orElse(List.of())
                .stream().map(TaskFolderInfraConvert.INSTANCE::toTaskFolder).toList();
    }

    @Override
    public List<TaskFolder> findAll() {
        List<TaskFolderDO> dos = taskFolderMapper.selectList(null);
        return Optional.ofNullable(dos).orElse(List.of())
                .stream().map(TaskFolderInfraConvert.INSTANCE::toTaskFolder).toList();
    }

    @Override
    public void deleteById(String id) {
        taskFolderMapper.deleteById(id);
    }
}
