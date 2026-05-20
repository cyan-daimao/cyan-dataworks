package com.cyan.dataworks.application.job_instance.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.JSON;
import com.cyan.dataworks.application.job_instance.JobInstanceService;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.cmd.JobInstanceCmd;
import com.cyan.dataworks.application.job_instance.convert.JobInstanceAppConvert;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.query.JobInstancePageQuery;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.infra.rpc.FlinkRpcClient;
import com.cyan.datagateway.client.SqlGatewayClient;
import com.cyan.datagateway.client.cmd.SqlExecuteCmd;
import com.cyan.datagateway.client.dto.SqlExecuteResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业实例应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class JobInstanceServiceImpl implements JobInstanceService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository jobInstanceRepository;
    private final SqlGatewayClient sqlGatewayClient;
    private final FlinkRpcClient flinkRpcClient;
    private final JobExecutionPlanner jobExecutionPlanner;

    public JobInstanceServiceImpl(JobRepository jobRepository,
                                  JobInstanceRepository jobInstanceRepository,
                                  SqlGatewayClient sqlGatewayClient,
                                  FlinkRpcClient flinkRpcClient,
                                  JobExecutionPlanner jobExecutionPlanner) {
        this.jobRepository = jobRepository;
        this.jobInstanceRepository = jobInstanceRepository;
        this.sqlGatewayClient = sqlGatewayClient;
        this.flinkRpcClient = flinkRpcClient;
        this.jobExecutionPlanner = jobExecutionPlanner;
    }

    /**
     * 手动执行作业，生成一个实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO execute(String jobId) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));

        String executableSql = jobExecutionPlanner.buildExecutableSql(job);
        JobInstanceCmd cmd = new JobInstanceCmd()
                .setJobId(jobId)
                .setJobName(job.getName())
                .setEngineType(job.getEngineType())
                .setSqlContent(executableSql)
                .setStatus(ExecutionStatus.RUNNING);

        JobInstance instance = JobInstanceAppConvert.INSTANCE.toJobInstance(cmd);
        instance.setCreatedAt(LocalDateTime.now());
        instance = instance.save(jobInstanceRepository);

        long startTime = System.currentTimeMillis();
        try {
            String resultData;
            if (job.getEngineType() == EngineType.SPARK) {
                resultData = executeSparkSql(executableSql);
            } else {
                resultData = executeFlinkSql(executableSql);
            }
            instance.markSuccess(resultData, System.currentTimeMillis() - startTime, jobInstanceRepository);
        } catch (Exception e) {
            instance.markFailed(e.getMessage(), System.currentTimeMillis() - startTime, jobInstanceRepository);
        }

        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }

    /**
     * 重试实例（基于原实例重新执行）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO retry(String instanceId) {
        JobInstance original = jobInstanceRepository.findById(instanceId);
        Assert.notNull(original, new SilentException("实例不存在"));

        JobInstanceCmd cmd = new JobInstanceCmd()
                .setJobId(original.getJobId())
                .setJobName(original.getJobName())
                .setEngineType(original.getEngineType())
                .setSqlContent(original.getSqlContent())
                .setStatus(ExecutionStatus.RUNNING);

        JobInstance instance = JobInstanceAppConvert.INSTANCE.toJobInstance(cmd);
        instance.setCreatedAt(LocalDateTime.now());
        instance = instance.save(jobInstanceRepository);

        long startTime = System.currentTimeMillis();
        try {
            String resultData;
            if (original.getEngineType() == EngineType.SPARK) {
                resultData = executeSparkSql(original.getSqlContent());
            } else {
                resultData = executeFlinkSql(original.getSqlContent());
            }
            instance.markSuccess(resultData, System.currentTimeMillis() - startTime, jobInstanceRepository);
        } catch (Exception e) {
            instance.markFailed(e.getMessage(), System.currentTimeMillis() - startTime, jobInstanceRepository);
        }

        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }

    /**
     * 终止运行中的实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO terminate(String instanceId) {
        JobInstance instance = jobInstanceRepository.findById(instanceId);
        Assert.notNull(instance, new SilentException("实例不存在"));
        instance.terminate(jobInstanceRepository);
        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }

    /**
     * 执行SparkSQL
     */
    private String executeSparkSql(String sql) {
        SqlExecuteCmd cmd = new SqlExecuteCmd();
        cmd.setSql(sql);
        Response<SqlExecuteResultDTO> response = sqlGatewayClient.executeSparkSql(cmd);
        if (response == null || response.getData() == null) {
            throw new SilentException("SparkSQL执行失败：无响应");
        }
        SqlExecuteResultDTO result = response.getData();
        if (result.getErrorMessage() != null && !result.getErrorMessage().isEmpty()) {
            throw new SilentException("SparkSQL执行失败：" + result.getErrorMessage());
        }
        return JSON.toJSONString(result.getData());
    }

    /**
     * 执行FlinkSQL
     */
    private String executeFlinkSql(String sql) {
        return flinkRpcClient.executeSql(sql);
    }

    /**
     * 分页查询实例
     */
    @Override
    public Page<JobInstanceBO> page(JobInstancePageQuery query) {
        Page<JobInstance> page = jobInstanceRepository.page(query);
        List<JobInstanceBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(JobInstanceAppConvert.INSTANCE::toJobInstanceBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据ID查询实例
     */
    @Override
    public JobInstanceBO findById(String id) {
        JobInstance instance = jobInstanceRepository.findById(id);
        Assert.notNull(instance, new SilentException("实例不存在"));
        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }
}
