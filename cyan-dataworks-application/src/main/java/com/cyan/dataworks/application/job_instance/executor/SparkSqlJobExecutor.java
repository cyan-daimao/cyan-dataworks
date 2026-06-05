package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.infra.remote.spark.operator.SparkApplicationOperatorService;
import com.cyan.dataworks.infra.remote.spark.operator.bo.SparkApplicationBO;
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
     * SparkApplication提交服务
     */
    private final SparkApplicationOperatorService sparkApplicationOperatorService;

    /**
     * 执行计划生成器
     */
    private final JobExecutionPlanner jobExecutionPlanner;

    public SparkSqlJobExecutor(SparkApplicationOperatorService sparkApplicationOperatorService, JobExecutionPlanner jobExecutionPlanner) {
        this.sparkApplicationOperatorService = sparkApplicationOperatorService;
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
        if ("preview".equals(instance.getId())) {
            throw new SilentException("SparkSQL临时执行暂不支持，请保存并发布后通过Spark Operator运行");
        }
        String executableSql = jobExecutionPlanner.buildExecutableSql(job);
        SparkApplicationBO application = sparkApplicationOperatorService.submitSparkSql(instance.getId(), executableSql, job.getConfigJson());
        return new JobExecutionResult()
                .setAsyncSubmitted(true)
                .setRuntimeJobName(application.getApplicationName())
                .setApplicationName(application.getApplicationName())
                .setApplicationNamespace(application.getNamespace())
                .setConfigMapName(application.getConfigMapName());
    }
}
