package com.cyan.dataworks.infra.persistence.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataworks.domain.workflow.WorkflowNode;
import com.cyan.dataworks.domain.workflow.repository.WorkflowNodeRepository;
import com.cyan.dataworks.infra.persistence.workflow.convert.WorkflowInfraConvert;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowNodeDO;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowNodeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流节点仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class WorkflowNodeRepositoryImpl implements WorkflowNodeRepository {

    /** 工作流节点Mapper */
    private final WorkflowNodeMapper workflowNodeMapper;

    public WorkflowNodeRepositoryImpl(WorkflowNodeMapper workflowNodeMapper) {
        this.workflowNodeMapper = workflowNodeMapper;
    }

    /**
     * 根据工作流ID查询节点
     */
    @Override
    public List<WorkflowNode> listByWorkflowId(String workflowId) {
        return Optional.ofNullable(workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNodeDO>()
                        .eq(WorkflowNodeDO::getWorkflowId, Convert.toLong(workflowId))
                        .orderByAsc(WorkflowNodeDO::getCreatedAt)))
                .orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toNode).toList();
    }

    /**
     * 根据ID查询节点
     */
    @Override
    public WorkflowNode findById(String id) {
        WorkflowNodeDO nodeDO = workflowNodeMapper.selectById(id);
        return nodeDO == null ? null : WorkflowInfraConvert.INSTANCE.toNode(nodeDO);
    }

    /**
     * 替换工作流节点
     */
    @Override
    public List<WorkflowNode> replaceByWorkflowId(String workflowId, List<WorkflowNode> nodes) {
        workflowNodeMapper.delete(new LambdaQueryWrapper<WorkflowNodeDO>()
                .eq(WorkflowNodeDO::getWorkflowId, Convert.toLong(workflowId)));
        for (WorkflowNode node : Optional.ofNullable(nodes).orElse(List.of())) {
            WorkflowNodeDO nodeDO = WorkflowInfraConvert.INSTANCE.toNodeDO(node);
            workflowNodeMapper.insert(nodeDO);
        }
        return listByWorkflowId(workflowId);
    }
}
