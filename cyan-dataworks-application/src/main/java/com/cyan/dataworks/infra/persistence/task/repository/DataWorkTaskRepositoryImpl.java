package com.cyan.dataworks.infra.persistence.task.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataworks.domain.task.DataWorkTask;
import com.cyan.dataworks.domain.task.query.DataWorkTaskPageQuery;
import com.cyan.dataworks.domain.task.repository.DataWorkTaskRepository;
import com.cyan.dataworks.infra.persistence.task.convert.TaskInfraConvert;
import com.cyan.dataworks.infra.persistence.task.dos.DataWorkTaskDO;
import com.cyan.dataworks.infra.persistence.task.mappers.DataWorkTaskMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工任务仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class DataWorkTaskRepositoryImpl implements DataWorkTaskRepository {

    private final DataWorkTaskMapper dataWorkTaskMapper;

    public DataWorkTaskRepositoryImpl(DataWorkTaskMapper dataWorkTaskMapper) {
        this.dataWorkTaskMapper = dataWorkTaskMapper;
    }

    /**
     * 分页查询任务
     */
    @Override
    public Page<DataWorkTask> page(DataWorkTaskPageQuery query) {
        LambdaQueryWrapper<DataWorkTaskDO> wrapper = new LambdaQueryWrapper<DataWorkTaskDO>()
                .like(StrUtils.isNotBlank(query.getName()), DataWorkTaskDO::getName, query.getName())
                .eq(query.getEngineType() != null, DataWorkTaskDO::getEngineType, query.getEngineType())
                .eq(StrUtils.isNotBlank(query.getCreatedBy()), DataWorkTaskDO::getCreatedBy, query.getCreatedBy())
                .eq(query.getFolderId() != null, DataWorkTaskDO::getFolderId, query.getFolderId())
                .orderByDesc(DataWorkTaskDO::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DataWorkTaskDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = dataWorkTaskMapper.selectPage(page, wrapper);
        List<DataWorkTask> data = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(TaskInfraConvert.INSTANCE::toDataWorkTask).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 列表查询任务
     */
    @Override
    public List<DataWorkTask> list(DataWorkTaskPageQuery query) {
        LambdaQueryWrapper<DataWorkTaskDO> wrapper = new LambdaQueryWrapper<DataWorkTaskDO>()
                .like(StrUtils.isNotBlank(query.getName()), DataWorkTaskDO::getName, query.getName())
                .eq(query.getEngineType() != null, DataWorkTaskDO::getEngineType, query.getEngineType())
                .eq(StrUtils.isNotBlank(query.getCreatedBy()), DataWorkTaskDO::getCreatedBy, query.getCreatedBy())
                .eq(query.getFolderId() != null, DataWorkTaskDO::getFolderId, query.getFolderId())
                .orderByDesc(DataWorkTaskDO::getCreatedAt);
        List<DataWorkTaskDO> dos = dataWorkTaskMapper.selectList(wrapper);
        return Optional.ofNullable(dos).orElse(List.of())
                .stream().map(TaskInfraConvert.INSTANCE::toDataWorkTask).toList();
    }

    /**
     * 根据ID查询任务
     */
    @Override
    public DataWorkTask findById(String id) {
        DataWorkTaskDO dataWorkTaskDO = dataWorkTaskMapper.selectById(id);
        if (dataWorkTaskDO == null) {
            return null;
        }
        return TaskInfraConvert.INSTANCE.toDataWorkTask(dataWorkTaskDO);
    }

    /**
     * 保存任务
     */
    @Override
    public DataWorkTask save(DataWorkTask task) {
        DataWorkTaskDO dataWorkTaskDO = TaskInfraConvert.INSTANCE.toDataWorkTaskDO(task);
        dataWorkTaskMapper.insert(dataWorkTaskDO);
        return findById(dataWorkTaskDO.getId() + "");
    }

    /**
     * 更新任务
     */
    @Override
    public DataWorkTask updateById(DataWorkTask task) {
        DataWorkTaskDO dataWorkTaskDO = TaskInfraConvert.INSTANCE.toDataWorkTaskDO(task);
        dataWorkTaskMapper.updateById(dataWorkTaskDO);
        return findById(task.getId());
    }

    /**
     * 删除任务
     */
    @Override
    public void deleteById(String id) {
        dataWorkTaskMapper.deleteById(id);
    }
}
