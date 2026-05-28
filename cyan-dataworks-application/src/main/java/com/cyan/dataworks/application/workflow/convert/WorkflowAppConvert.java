package com.cyan.dataworks.application.workflow.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.workflow.bo.WorkflowBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowInstanceBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowScheduleBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowCmd;
import com.cyan.dataworks.domain.workflow.Workflow;
import com.cyan.dataworks.domain.workflow.WorkflowEdge;
import com.cyan.dataworks.domain.workflow.WorkflowInstance;
import com.cyan.dataworks.domain.workflow.WorkflowNode;
import com.cyan.dataworks.domain.workflow.WorkflowSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 工作流应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface WorkflowAppConvert {

    WorkflowAppConvert INSTANCE = Mappers.getMapper(WorkflowAppConvert.class);

    /** Domain转BO */
    WorkflowBO toWorkflowBO(Workflow workflow);

    /** Cmd转Domain */
    Workflow toWorkflow(WorkflowCmd cmd);

    /** 节点Domain转BO */
    WorkflowDefinitionBO.NodeBO toNodeBO(WorkflowNode node);

    /** 依赖边Domain转BO */
    WorkflowDefinitionBO.EdgeBO toEdgeBO(WorkflowEdge edge);

    /** 调度Domain转BO */
    WorkflowScheduleBO toScheduleBO(WorkflowSchedule schedule);

    /** 实例Domain转BO */
    WorkflowInstanceBO toInstanceBO(WorkflowInstance instance);
}
