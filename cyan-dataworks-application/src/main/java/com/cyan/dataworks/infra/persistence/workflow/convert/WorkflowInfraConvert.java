package com.cyan.dataworks.infra.persistence.workflow.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.workflow.Workflow;
import com.cyan.dataworks.domain.workflow.WorkflowDependency;
import com.cyan.dataworks.domain.workflow.WorkflowEdge;
import com.cyan.dataworks.domain.workflow.WorkflowInstance;
import com.cyan.dataworks.domain.workflow.WorkflowNode;
import com.cyan.dataworks.domain.workflow.WorkflowSchedule;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowDependencyDO;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowDO;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowEdgeDO;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowInstanceDO;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowNodeDO;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowScheduleDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 工作流基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface WorkflowInfraConvert {

    WorkflowInfraConvert INSTANCE = Mappers.getMapper(WorkflowInfraConvert.class);

    /**
     * DO转工作流
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(workflowDO.getId()))")
    Workflow toWorkflow(WorkflowDO workflowDO);

    /**
     * 工作流转DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(workflow.getId()))")
    WorkflowDO toWorkflowDO(Workflow workflow);

    /**
     * DO转节点
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(nodeDO.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toStr(nodeDO.getWorkflowId()))")
    @Mapping(target = "jobId", expression = "java(com.cyan.arch.common.util.Convert.toStr(nodeDO.getJobId()))")
    WorkflowNode toNode(WorkflowNodeDO nodeDO);

    /**
     * 节点转DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(node.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toLong(node.getWorkflowId()))")
    @Mapping(target = "jobId", expression = "java(com.cyan.arch.common.util.Convert.toLong(node.getJobId()))")
    WorkflowNodeDO toNodeDO(WorkflowNode node);

    /**
     * DO转依赖边
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(edgeDO.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toStr(edgeDO.getWorkflowId()))")
    @Mapping(target = "upstreamNodeId", expression = "java(com.cyan.arch.common.util.Convert.toStr(edgeDO.getUpstreamNodeId()))")
    @Mapping(target = "downstreamNodeId", expression = "java(com.cyan.arch.common.util.Convert.toStr(edgeDO.getDownstreamNodeId()))")
    WorkflowEdge toEdge(WorkflowEdgeDO edgeDO);

    /**
     * 依赖边转DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(edge.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toLong(edge.getWorkflowId()))")
    @Mapping(target = "upstreamNodeId", expression = "java(com.cyan.arch.common.util.Convert.toLong(edge.getUpstreamNodeId()))")
    @Mapping(target = "downstreamNodeId", expression = "java(com.cyan.arch.common.util.Convert.toLong(edge.getDownstreamNodeId()))")
    WorkflowEdgeDO toEdgeDO(WorkflowEdge edge);

    /**
     * DO转工作流级依赖
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(dependencyDO.getId()))")
    @Mapping(target = "upstreamWorkflowId", expression = "java(com.cyan.arch.common.util.Convert.toStr(dependencyDO.getUpstreamWorkflowId()))")
    @Mapping(target = "downstreamWorkflowId", expression = "java(com.cyan.arch.common.util.Convert.toStr(dependencyDO.getDownstreamWorkflowId()))")
    WorkflowDependency toDependency(WorkflowDependencyDO dependencyDO);

    /**
     * 工作流级依赖转DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(dependency.getId()))")
    @Mapping(target = "upstreamWorkflowId", expression = "java(com.cyan.arch.common.util.Convert.toLong(dependency.getUpstreamWorkflowId()))")
    @Mapping(target = "downstreamWorkflowId", expression = "java(com.cyan.arch.common.util.Convert.toLong(dependency.getDownstreamWorkflowId()))")
    WorkflowDependencyDO toDependencyDO(WorkflowDependency dependency);

    /**
     * DO转调度配置
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(scheduleDO.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toStr(scheduleDO.getWorkflowId()))")
    WorkflowSchedule toSchedule(WorkflowScheduleDO scheduleDO);

    /**
     * 调度配置转DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(schedule.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toLong(schedule.getWorkflowId()))")
    WorkflowScheduleDO toScheduleDO(WorkflowSchedule schedule);

    /**
     * DO转实例
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(instanceDO.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toStr(instanceDO.getWorkflowId()))")
    WorkflowInstance toInstance(WorkflowInstanceDO instanceDO);

    /**
     * 实例转DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(instance.getId()))")
    @Mapping(target = "workflowId", expression = "java(com.cyan.arch.common.util.Convert.toLong(instance.getWorkflowId()))")
    WorkflowInstanceDO toInstanceDO(WorkflowInstance instance);
}
