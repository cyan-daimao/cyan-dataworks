package com.cyan.dataworks.adapter.workflow.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.workflow.http.dto.AirflowDagRunDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.AirflowTaskInstanceDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowDagDefinitionDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowDefinitionDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowInstanceDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowScheduleDTO;
import com.cyan.dataworks.application.workflow.bo.AirflowDagRunBO;
import com.cyan.dataworks.application.workflow.bo.AirflowTaskInstanceBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDagDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowInstanceBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowScheduleBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 工作流适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface WorkflowAdapterConvert {

    WorkflowAdapterConvert INSTANCE = Mappers.getMapper(WorkflowAdapterConvert.class);

    /** 工作流BO转DTO */
    WorkflowDTO toWorkflowDTO(WorkflowBO bo);

    /** 工作流定义BO转DTO */
    WorkflowDefinitionDTO toDefinitionDTO(WorkflowDefinitionBO bo);

    /** 工作流DAG定义BO转DTO */
    WorkflowDagDefinitionDTO toDagDefinitionDTO(WorkflowDagDefinitionBO bo);

    /** 调度配置BO转DTO */
    WorkflowScheduleDTO toScheduleDTO(WorkflowScheduleBO bo);

    /** 工作流实例BO转DTO */
    WorkflowInstanceDTO toInstanceDTO(WorkflowInstanceBO bo);

    /** Airflow DAG Run BO转DTO */
    AirflowDagRunDTO toDagRunDTO(AirflowDagRunBO bo);

    /** Airflow任务实例BO转DTO */
    AirflowTaskInstanceDTO toTaskInstanceDTO(AirflowTaskInstanceBO bo);
}
