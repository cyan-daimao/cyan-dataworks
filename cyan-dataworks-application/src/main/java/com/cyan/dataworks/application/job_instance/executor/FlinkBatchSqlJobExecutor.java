package com.cyan.dataworks.application.job_instance.executor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.runtime.FlinkRuntimeConfig;
import com.cyan.dataworks.application.job.runtime.FlinkRuntimeConfigParser;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.SqlPolicy;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.infra.remote.flink.FlinkRemoteService;
import org.springframework.stereotype.Component;

/**
 * FlinkSQL批任务执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class FlinkBatchSqlJobExecutor implements JobExecutor {

    /**
     * Flink远程服务
     */
    private final FlinkRemoteService flinkRemoteService;

    /**
     * 执行计划生成器
     */
    private final JobExecutionPlanner jobExecutionPlanner;

    /**
     * Flink运行配置解析器
     */
    private final FlinkRuntimeConfigParser flinkRuntimeConfigParser;

    public FlinkBatchSqlJobExecutor(FlinkRemoteService flinkRemoteService,
                                    JobExecutionPlanner jobExecutionPlanner,
                                    FlinkRuntimeConfigParser flinkRuntimeConfigParser) {
        this.flinkRemoteService = flinkRemoteService;
        this.jobExecutionPlanner = jobExecutionPlanner;
        this.flinkRuntimeConfigParser = flinkRuntimeConfigParser;
    }

    /**
     * 是否支持节点类型
     */
    @Override
    public boolean supports(NodeType nodeType) {
        return nodeType == NodeType.FLINK_BATCH;
    }

    /**
     * 执行FlinkSQL批任务
     */
    @Override
    public JobExecutionResult execute(Job job, JobInstance instance) {
        if ("preview".equals(instance.getId())) {
            throw new SilentException("FlinkSQL批任务临时执行暂不支持，请保存并发布后通过Airflow调度运行");
        }
        SqlPolicy.assertFlinkApplicationSql(job.getContent());
        String executableSql = jobExecutionPlanner.buildExecutableSql(job);
        FlinkRuntimeConfig runtimeConfig = flinkRuntimeConfigParser.parse(job);
        String resultData = flinkRemoteService.submitApplication(instance.getId(), job.getName(), executableSql, runtimeConfig);
        JSONObject resultNode = parseResultData(resultData);
        String deploymentName = resultNode.getString("deploymentName");
        String namespace = resultNode.getString("namespace");
        String configMapName = resultNode.getString("configMapName");
        return new JobExecutionResult()
                .setResultData(resultData)
                .setAsyncSubmitted(true)
                .setRuntimeJobName(deploymentName)
                .setApplicationName(deploymentName)
                .setApplicationNamespace(namespace)
                .setConfigMapName(configMapName);
    }

    /**
     * 解析提交结果
     */
    private JSONObject parseResultData(String resultData) {
        try {
            return JSON.parseObject(resultData);
        } catch (Exception e) {
            throw new SilentException("FlinkSQL批任务提交结果解析失败：" + e.getMessage());
        }
    }
}
