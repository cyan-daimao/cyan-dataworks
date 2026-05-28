package com.cyan.dataworks.application.workflow.bo;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工作流DAG定义业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDagDefinitionBO {

    /** DAG ID */
    private String dagId;

    /** 工作流ID */
    private String workflowId;

    /** 工作流名称 */
    private String workflowName;

    /** Cron表达式 */
    private String cronExpression;

    /** 任务列表 */
    private List<TaskBO> tasks;

    /**
     * Airflow任务业务对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class TaskBO {

        /** 任务ID */
        private String taskId;

        /** 节点ID */
        private String nodeId;

        /** 作业ID */
        private String jobId;

        /** 作业名称 */
        private String jobName;

        /** 引擎类型 */
        private EngineType engineType;

        /** 节点类型 */
        private NodeType nodeType;

        /** 上游任务ID列表 */
        private List<String> upstreamTaskIds;
    }
}
