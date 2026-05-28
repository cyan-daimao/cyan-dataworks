package com.cyan.dataworks.infra.persistence.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataworks.domain.workflow.WorkflowInstance;
import com.cyan.dataworks.domain.workflow.query.WorkflowInstancePageQuery;
import com.cyan.dataworks.domain.workflow.repository.WorkflowInstanceRepository;
import com.cyan.dataworks.infra.persistence.workflow.convert.WorkflowInfraConvert;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowInstanceDO;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowInstanceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流实例仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class WorkflowInstanceRepositoryImpl implements WorkflowInstanceRepository {

    /** 工作流实例Mapper */
    private final WorkflowInstanceMapper workflowInstanceMapper;

    public WorkflowInstanceRepositoryImpl(WorkflowInstanceMapper workflowInstanceMapper) {
        this.workflowInstanceMapper = workflowInstanceMapper;
    }

    /**
     * 分页查询工作流实例
     */
    @Override
    public Page<WorkflowInstance> page(WorkflowInstancePageQuery query) {
        LambdaQueryWrapper<WorkflowInstanceDO> wrapper = new LambdaQueryWrapper<WorkflowInstanceDO>()
                .eq(query.getWorkflowId() != null, WorkflowInstanceDO::getWorkflowId, Convert.toLong(query.getWorkflowId()))
                .eq(query.getStatus() != null, WorkflowInstanceDO::getStatus, query.getStatus())
                .orderByDesc(WorkflowInstanceDO::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkflowInstanceDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = workflowInstanceMapper.selectPage(page, wrapper);
        List<WorkflowInstance> data = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toInstance).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据ID查询实例
     */
    @Override
    public WorkflowInstance findById(String id) {
        WorkflowInstanceDO instanceDO = workflowInstanceMapper.selectById(id);
        return instanceDO == null ? null : WorkflowInfraConvert.INSTANCE.toInstance(instanceDO);
    }

    /**
     * 根据DAG Run查询实例
     */
    @Override
    public WorkflowInstance findByDagRun(String dagId, String dagRunId) {
        WorkflowInstanceDO instanceDO = workflowInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowInstanceDO>()
                .eq(WorkflowInstanceDO::getDagId, dagId)
                .eq(WorkflowInstanceDO::getDagRunId, dagRunId)
                .last("LIMIT 1"));
        return instanceDO == null ? null : WorkflowInfraConvert.INSTANCE.toInstance(instanceDO);
    }

    /**
     * 保存实例
     */
    @Override
    public WorkflowInstance save(WorkflowInstance instance) {
        WorkflowInstanceDO instanceDO = WorkflowInfraConvert.INSTANCE.toInstanceDO(instance);
        workflowInstanceMapper.insert(instanceDO);
        return findById(instanceDO.getId() + "");
    }

    /**
     * 更新实例
     */
    @Override
    public WorkflowInstance updateById(WorkflowInstance instance) {
        WorkflowInstanceDO instanceDO = WorkflowInfraConvert.INSTANCE.toInstanceDO(instance);
        workflowInstanceMapper.updateById(instanceDO);
        return findById(instance.getId());
    }
}
