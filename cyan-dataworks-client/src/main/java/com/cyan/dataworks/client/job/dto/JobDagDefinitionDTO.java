package com.cyan.dataworks.client.job.dto;

import com.cyan.dataworks.client.enums.EngineType;
import com.cyan.dataworks.client.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 单节点作业DAG定义DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDagDefinitionDTO {

    /** DAG ID */
    private String dagId;

    /** 作业ID */
    private String jobId;

    /** 作业名称 */
    private String jobName;

    /** Cron表达式 */
    private String cronExpression;

    /** 调度是否启用 */
    private Boolean scheduleEnabled;

    /** 引擎类型 */
    private EngineType engineType;

    /** 节点类型 */
    private NodeType nodeType;

    /** 外部作业依赖列表 */
    private List<ExternalDependencyDTO> externalDependencies;

    /**
     * 外部作业依赖DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class ExternalDependencyDTO {

        /** 上游DAG ID */
        private String upstreamDagId;

        /** 上游作业ID */
        private String upstreamJobId;

        /** 上游作业名称 */
        private String upstreamJobName;
    }
}
