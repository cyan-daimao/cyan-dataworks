package com.cyan.dataworks.application.job.bo;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 单节点作业DAG定义业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDagDefinitionBO {

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
    private List<ExternalDependencyBO> externalDependencies;

    /**
     * 外部作业依赖业务对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class ExternalDependencyBO {

        /** 上游DAG ID */
        private String upstreamDagId;

        /** 上游作业ID */
        private String upstreamJobId;

        /** 上游作业名称 */
        private String upstreamJobName;
    }
}
