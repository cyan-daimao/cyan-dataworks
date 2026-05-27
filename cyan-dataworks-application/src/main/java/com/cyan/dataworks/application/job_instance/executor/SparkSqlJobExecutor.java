package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.JSON;
import com.cyan.datagateway.client.SqlGatewayClient;
import com.cyan.datagateway.client.cmd.SqlExecuteCmd;
import com.cyan.datagateway.client.dto.SqlExecuteResultDTO;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;
import org.springframework.stereotype.Component;

/**
 * SparkSQL作业执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class SparkSqlJobExecutor implements JobExecutor {

    /**
     * SQL网关客户端
     */
    private final SqlGatewayClient sqlGatewayClient;

    /**
     * 执行计划生成器
     */
    private final JobExecutionPlanner jobExecutionPlanner;

    public SparkSqlJobExecutor(SqlGatewayClient sqlGatewayClient, JobExecutionPlanner jobExecutionPlanner) {
        this.sqlGatewayClient = sqlGatewayClient;
        this.jobExecutionPlanner = jobExecutionPlanner;
    }

    /**
     * 是否支持节点类型
     */
    @Override
    public boolean supports(NodeType nodeType) {
        return nodeType == NodeType.SPARK_SQL;
    }

    /**
     * 执行SparkSQL
     */
    @Override
    public JobExecutionResult execute(Job job, JobInstance instance) {
        String executableSql = jobExecutionPlanner.buildExecutableSql(job);
        SqlExecuteCmd cmd = new SqlExecuteCmd();
        cmd.setSql(executableSql);
        Response<SqlExecuteResultDTO> response = sqlGatewayClient.executeSparkSql(cmd);
        if (response == null || response.getData() == null) {
            throw new SilentException("SparkSQL执行失败：无响应");
        }
        SqlExecuteResultDTO result = response.getData();
        if (result.getErrorMessage() != null && !result.getErrorMessage().isEmpty()) {
            throw new SilentException("SparkSQL执行失败：" + result.getErrorMessage());
        }
        return new JobExecutionResult().setResultData(JSON.toJSONString(result.getData()));
    }
}
