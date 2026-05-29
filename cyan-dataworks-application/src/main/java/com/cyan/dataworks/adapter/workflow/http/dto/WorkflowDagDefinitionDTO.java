package com.cyan.dataworks.adapter.workflow.http.dto;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工作流DAG定义DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDagDefinitionDTO {

    /** DAG ID */
    private String dagId;

    /** 工作流ID */
    private String workflowId;

    /** 工作流名称 */
    private String workflowName;

    /** Cron表达式 */
    private String cronExpression;

    /** 调度是否启用 */
    private Boolean scheduleEnabled;

    /** 任务列表 */
    private List<TaskDTO> tasks;

    /** 外部工作流依赖列表 */
    private List<ExternalDependencyDTO> externalDependencies;

    /**
     * DAG任务DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class TaskDTO {

        /** 任务ID */
        private String taskId;

        /** 节点ID */
        private String nodeId;

        /** 节点名称 */
        private String nodeName;

        /** 引擎类型 */
        private EngineType engineType;

        /** 节点类型 */
        private NodeType nodeType;

        /** 上游任务ID列表 */
        private List<String> upstreamTaskIds;
    }

    /**
     * 外部工作流依赖DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class ExternalDependencyDTO {

        /** 上游DAG ID */
        private String upstreamDagId;

        /** 上游工作流ID */
        private String upstreamWorkflowId;

        /** 上游工作流名称 */
        private String upstreamWorkflowName;
    }
}
