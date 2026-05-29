package com.cyan.dataworks.infra.persistence.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataworks.domain.workflow.Workflow;
import com.cyan.dataworks.domain.workflow.query.WorkflowPageQuery;
import com.cyan.dataworks.domain.workflow.repository.WorkflowRepository;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.infra.persistence.workflow.convert.WorkflowInfraConvert;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowDO;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowScheduleDO;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowMapper;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowScheduleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {

    /** 工作流Mapper */
    private final WorkflowMapper workflowMapper;

    /** 工作流调度Mapper */
    private final WorkflowScheduleMapper workflowScheduleMapper;

    public WorkflowRepositoryImpl(WorkflowMapper workflowMapper,
                                  WorkflowScheduleMapper workflowScheduleMapper) {
        this.workflowMapper = workflowMapper;
        this.workflowScheduleMapper = workflowScheduleMapper;
    }

    /**
     * 分页查询工作流
     */
    @Override
    public Page<Workflow> page(WorkflowPageQuery query) {
        LambdaQueryWrapper<WorkflowDO> wrapper = new LambdaQueryWrapper<WorkflowDO>()
                .like(StrUtils.isNotBlank(query.getName()), WorkflowDO::getName, query.getName())
                .eq(query.getStatus() != null, WorkflowDO::getStatus, query.getStatus())
                .eq(StrUtils.isNotBlank(query.getCreatedBy()), WorkflowDO::getCreatedBy, query.getCreatedBy())
                .orderByDesc(WorkflowDO::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkflowDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = workflowMapper.selectPage(page, wrapper);
        List<Workflow> data = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toWorkflow).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据ID查询工作流
     */
    @Override
    public Workflow findById(String id) {
        WorkflowDO workflowDO = workflowMapper.selectById(id);
        return workflowDO == null ? null : WorkflowInfraConvert.INSTANCE.toWorkflow(workflowDO);
    }

    /**
     * 根据DAG ID查询工作流
     */
    @Override
    public Workflow findByDagId(String dagId) {
        WorkflowDO workflowDO = workflowMapper.selectOne(new LambdaQueryWrapper<WorkflowDO>()
                .eq(WorkflowDO::getDagId, dagId)
                .last("LIMIT 1"));
        return workflowDO == null ? null : WorkflowInfraConvert.INSTANCE.toWorkflow(workflowDO);
    }

    /**
     * 查询Airflow工作流
     */
    @Override
    public List<Workflow> listAirflowWorkflows() {
        List<Long> workflowIds = Optional.ofNullable(workflowScheduleMapper.selectList(new LambdaQueryWrapper<WorkflowScheduleDO>()
                        .eq(WorkflowScheduleDO::getSchedulerType, SchedulerType.AIRFLOW)))
                .orElse(List.of())
                .stream().map(WorkflowScheduleDO::getWorkflowId).distinct().toList();
        if (workflowIds.isEmpty()) {
            return List.of();
        }
        return Optional.ofNullable(workflowMapper.selectList(new LambdaQueryWrapper<WorkflowDO>()
                        .in(WorkflowDO::getId, workflowIds)))
                .orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toWorkflow).toList();
    }

    /**
     * 保存工作流
     */
    @Override
    public Workflow save(Workflow workflow) {
        WorkflowDO workflowDO = WorkflowInfraConvert.INSTANCE.toWorkflowDO(workflow);
        workflowMapper.insert(workflowDO);
        return findById(workflowDO.getId() + "");
    }

    /**
     * 更新工作流
     */
    @Override
    public Workflow updateById(Workflow workflow) {
        WorkflowDO workflowDO = WorkflowInfraConvert.INSTANCE.toWorkflowDO(workflow);
        workflowMapper.updateById(workflowDO);
        return findById(workflow.getId());
    }

    /**
     * 删除工作流
     */
    @Override
    public void deleteById(String id) {
        workflowMapper.deleteById(id);
    }
}
