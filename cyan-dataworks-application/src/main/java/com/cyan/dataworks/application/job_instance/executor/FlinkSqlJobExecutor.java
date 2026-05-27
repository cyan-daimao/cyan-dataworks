package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.infra.remote.flink.FlinkRemoteService;
import org.springframework.stereotype.Component;

/**
 * FlinkSQL作业执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class FlinkSqlJobExecutor implements JobExecutor {

    /**
     * Flink远程服务
     */
    private final FlinkRemoteService flinkRemoteService;

    /**
     * 执行计划生成器
     */
    private final JobExecutionPlanner jobExecutionPlanner;

    public FlinkSqlJobExecutor(FlinkRemoteService flinkRemoteService, JobExecutionPlanner jobExecutionPlanner) {
        this.flinkRemoteService = flinkRemoteService;
        this.jobExecutionPlanner = jobExecutionPlanner;
    }

    /**
     * 是否支持节点类型
     */
    @Override
    public boolean supports(NodeType nodeType) {
        return nodeType == NodeType.FLINK_SQL;
    }

    /**
     * 执行FlinkSQL
     */
    @Override
    public JobExecutionResult execute(Job job, JobInstance instance) {
        String executableSql = jobExecutionPlanner.buildExecutableSql(job);
        return new JobExecutionResult().setResultData(flinkRemoteService.executeSql(executableSql));
    }
}
