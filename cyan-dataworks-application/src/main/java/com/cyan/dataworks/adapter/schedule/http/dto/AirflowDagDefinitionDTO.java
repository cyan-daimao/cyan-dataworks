package com.cyan.dataworks.adapter.schedule.http.dto;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Airflow DAG定义DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AirflowDagDefinitionDTO {

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 任务列表
     */
    private List<TaskDTO> tasks;

    /**
     * Airflow任务DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class TaskDTO {

        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 作业ID
         */
        private String jobId;

        /**
         * 作业名称
         */
        private String jobName;

        /**
         * 引擎类型
         */
        private EngineType engineType;

        /**
         * 节点类型
         */
        private NodeType nodeType;

        /**
         * 上游任务ID列表
         */
        private List<String> upstreamTaskIds;
    }
}
