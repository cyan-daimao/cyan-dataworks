package com.cyan.dataworks.infra.persistence.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataworks.domain.workflow.WorkflowEdge;
import com.cyan.dataworks.domain.workflow.repository.WorkflowEdgeRepository;
import com.cyan.dataworks.infra.persistence.workflow.convert.WorkflowInfraConvert;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowEdgeDO;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowEdgeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流依赖边仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class WorkflowEdgeRepositoryImpl implements WorkflowEdgeRepository {

    /** 工作流依赖边Mapper */
    private final WorkflowEdgeMapper workflowEdgeMapper;

    public WorkflowEdgeRepositoryImpl(WorkflowEdgeMapper workflowEdgeMapper) {
        this.workflowEdgeMapper = workflowEdgeMapper;
    }

    /**
     * 根据工作流ID查询依赖边
     */
    @Override
    public List<WorkflowEdge> listByWorkflowId(String workflowId) {
        return Optional.ofNullable(workflowEdgeMapper.selectList(new LambdaQueryWrapper<WorkflowEdgeDO>()
                        .eq(WorkflowEdgeDO::getWorkflowId, Convert.toLong(workflowId))
                        .orderByAsc(WorkflowEdgeDO::getCreatedAt)))
                .orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toEdge).toList();
    }

    /**
     * 替换工作流依赖边
     */
    @Override
    public List<WorkflowEdge> replaceByWorkflowId(String workflowId, List<WorkflowEdge> edges) {
        workflowEdgeMapper.softDeleteByWorkflowId(Convert.toLong(workflowId));
        for (WorkflowEdge edge : Optional.ofNullable(edges).orElse(List.of())) {
            WorkflowEdgeDO edgeDO = WorkflowInfraConvert.INSTANCE.toEdgeDO(edge);
            workflowEdgeMapper.insert(edgeDO);
        }
        return listByWorkflowId(workflowId);
    }
}
