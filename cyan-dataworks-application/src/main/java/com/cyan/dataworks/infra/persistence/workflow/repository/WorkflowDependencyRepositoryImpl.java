package com.cyan.dataworks.infra.persistence.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataworks.domain.workflow.WorkflowDependency;
import com.cyan.dataworks.domain.workflow.repository.WorkflowDependencyRepository;
import com.cyan.dataworks.infra.persistence.workflow.convert.WorkflowInfraConvert;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowDependencyDO;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowDependencyMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流级依赖仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class WorkflowDependencyRepositoryImpl implements WorkflowDependencyRepository {

    /** 工作流级依赖Mapper */
    private final WorkflowDependencyMapper workflowDependencyMapper;

    public WorkflowDependencyRepositoryImpl(WorkflowDependencyMapper workflowDependencyMapper) {
        this.workflowDependencyMapper = workflowDependencyMapper;
    }

    /** 查询工作流上游依赖 */
    @Override
    public List<WorkflowDependency> listByDownstreamWorkflowId(String downstreamWorkflowId) {
        LambdaQueryWrapper<WorkflowDependencyDO> wrapper = new LambdaQueryWrapper<WorkflowDependencyDO>()
                .eq(WorkflowDependencyDO::getDownstreamWorkflowId, Convert.toLong(downstreamWorkflowId))
                .orderByAsc(WorkflowDependencyDO::getCreatedAt);
        return toDomains(workflowDependencyMapper.selectList(wrapper));
    }

    /** 查询工作流下游依赖 */
    @Override
    public List<WorkflowDependency> listByUpstreamWorkflowId(String upstreamWorkflowId) {
        LambdaQueryWrapper<WorkflowDependencyDO> wrapper = new LambdaQueryWrapper<WorkflowDependencyDO>()
                .eq(WorkflowDependencyDO::getUpstreamWorkflowId, Convert.toLong(upstreamWorkflowId))
                .orderByAsc(WorkflowDependencyDO::getCreatedAt);
        return toDomains(workflowDependencyMapper.selectList(wrapper));
    }

    /** 查询全部工作流依赖 */
    @Override
    public List<WorkflowDependency> listAll() {
        LambdaQueryWrapper<WorkflowDependencyDO> wrapper = new LambdaQueryWrapper<WorkflowDependencyDO>()
                .orderByAsc(WorkflowDependencyDO::getCreatedAt);
        return toDomains(workflowDependencyMapper.selectList(wrapper));
    }

    /** 替换工作流上游依赖 */
    @Override
    public List<WorkflowDependency> replaceByDownstreamWorkflowId(String downstreamWorkflowId, List<WorkflowDependency> dependencies) {
        workflowDependencyMapper.delete(new LambdaQueryWrapper<WorkflowDependencyDO>()
                .eq(WorkflowDependencyDO::getDownstreamWorkflowId, Convert.toLong(downstreamWorkflowId)));
        for (WorkflowDependency dependency : Optional.ofNullable(dependencies).orElse(List.of())) {
            workflowDependencyMapper.insert(WorkflowInfraConvert.INSTANCE.toDependencyDO(dependency));
        }
        return listByDownstreamWorkflowId(downstreamWorkflowId);
    }

    /** 删除工作流相关依赖 */
    @Override
    public void deleteByWorkflowId(String workflowId) {
        Long id = Convert.toLong(workflowId);
        workflowDependencyMapper.delete(new LambdaQueryWrapper<WorkflowDependencyDO>()
                .eq(WorkflowDependencyDO::getUpstreamWorkflowId, id)
                .or()
                .eq(WorkflowDependencyDO::getDownstreamWorkflowId, id));
    }

    /** 转换领域对象列表 */
    private List<WorkflowDependency> toDomains(List<WorkflowDependencyDO> dependencyDOS) {
        return Optional.ofNullable(dependencyDOS).orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toDependency).toList();
    }
}
