package com.cyan.dataworks.application.task.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.task.TaskService;
import com.cyan.dataworks.application.task.bo.DataWorkTaskBO;
import com.cyan.dataworks.application.task.cmd.DataWorkTaskCmd;
import com.cyan.dataworks.application.task.convert.TaskAppConvert;
import com.cyan.dataworks.domain.schedule.repository.ScheduleConfigRepository;
import com.cyan.dataworks.domain.task.DataWorkTask;
import com.cyan.dataworks.domain.task.query.DataWorkTaskPageQuery;
import com.cyan.dataworks.domain.task.repository.DataWorkTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工任务应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class TaskServiceImpl implements TaskService {

    private final DataWorkTaskRepository dataWorkTaskRepository;
    private final ScheduleConfigRepository scheduleConfigRepository;
    private final com.cyan.dataworks.infra.schedule.ScheduleJobExecutor scheduleJobExecutor;

    public TaskServiceImpl(DataWorkTaskRepository dataWorkTaskRepository,
                           ScheduleConfigRepository scheduleConfigRepository,
                           com.cyan.dataworks.infra.schedule.ScheduleJobExecutor scheduleJobExecutor) {
        this.dataWorkTaskRepository = dataWorkTaskRepository;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.scheduleJobExecutor = scheduleJobExecutor;
    }

    /**
     * 分页查询任务
     */
    @Override
    public Page<DataWorkTaskBO> page(DataWorkTaskPageQuery query) {
        Page<DataWorkTask> page = dataWorkTaskRepository.page(query);
        List<DataWorkTaskBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(TaskAppConvert.INSTANCE::toDataWorkTaskBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 列表查询任务
     */
    @Override
    public List<DataWorkTaskBO> list(DataWorkTaskPageQuery query) {
        List<DataWorkTask> tasks = dataWorkTaskRepository.list(query);
        return Optional.ofNullable(tasks).orElse(List.of())
                .stream().map(TaskAppConvert.INSTANCE::toDataWorkTaskBO).toList();
    }

    /**
     * 根据ID查询任务
     */
    @Override
    public DataWorkTaskBO findById(String id) {
        DataWorkTask task = dataWorkTaskRepository.findById(id);
        Assert.notNull(task, new SilentException("任务不存在"));
        return TaskAppConvert.INSTANCE.toDataWorkTaskBO(task);
    }

    /**
     * 保存任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataWorkTaskBO save(DataWorkTaskCmd cmd, String createdBy) {
        DataWorkTask task = TaskAppConvert.INSTANCE.toDataWorkTask(cmd);
        task.setCreatedBy(createdBy);
        task = task.save(dataWorkTaskRepository);
        return TaskAppConvert.INSTANCE.toDataWorkTaskBO(task);
    }

    /**
     * 更新任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataWorkTaskBO update(String id, DataWorkTaskCmd cmd) {
        DataWorkTask existing = dataWorkTaskRepository.findById(id);
        Assert.notNull(existing, new SilentException("任务不存在"));
        DataWorkTask task = TaskAppConvert.INSTANCE.toDataWorkTask(cmd);
        task.setId(id);
        task = task.update(dataWorkTaskRepository);
        return TaskAppConvert.INSTANCE.toDataWorkTaskBO(task);
    }

    /**
     * 删除任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        DataWorkTask existing = dataWorkTaskRepository.findById(id);
        Assert.notNull(existing, new SilentException("任务不存在"));
        existing.delete(dataWorkTaskRepository);
        scheduleConfigRepository.deleteByTaskId(id);
        scheduleJobExecutor.cancel(id);
    }
}
