package com.cyan.dataworks.application.job_instance.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job_instance.JobInstanceService;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceLogBO;
import com.cyan.dataworks.application.job_instance.cmd.JobInstanceCallbackCmd;
import com.cyan.dataworks.application.job_instance.cmd.JobInstanceCmd;
import com.cyan.dataworks.application.job_instance.cmd.JobPreviewExecuteCmd;
import com.cyan.dataworks.application.job_instance.cmd.JobRunBySchedulerCmd;
import com.cyan.dataworks.application.job_instance.convert.JobInstanceAppConvert;
import com.cyan.dataworks.application.job_instance.executor.JobExecutionResult;
import com.cyan.dataworks.application.job_instance.executor.JobExecutorRegistry;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.application.job.runtime.FlinkRuntimeConfig;
import com.cyan.dataworks.application.job.runtime.FlinkRuntimeConfigParser;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.query.JobInstanceLogQuery;
import com.cyan.dataworks.domain.job_instance.query.JobInstancePageQuery;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.domain.workflow.WorkflowInstance;
import com.cyan.dataworks.domain.workflow.repository.WorkflowInstanceRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowNodeRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.JobLogRole;
import com.cyan.dataworks.infra.config.ScriptRuntimeProperties;
import com.cyan.dataworks.infra.remote.airflow.AirflowRemoteLogService;
import com.cyan.dataworks.infra.remote.flink.FlinkRemoteService;
import com.cyan.dataworks.infra.remote.flink.operator.bo.FlinkPodLogBO;
import com.cyan.dataworks.infra.remote.rustfs.RustFsLogService;
import com.cyan.dataworks.infra.remote.spark.operator.SparkApplicationOperatorService;
import com.cyan.dataworks.infra.remote.spark.operator.bo.SparkApplicationBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final FlinkRemoteService flinkRemoteService;
    private final JobExecutionPlanner jobExecutionPlanner;
    private final FlinkRuntimeConfigParser flinkRuntimeConfigParser;
    private final JobExecutorRegistry jobExecutorRegistry;
    private final AirflowRemoteLogService airflowRemoteLogService;
    private final RustFsLogService rustFsLogService;
    private final SparkApplicationOperatorService sparkApplicationOperatorService;
    private final ScriptRuntimeProperties scriptRuntimeProperties;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final ObjectMapper objectMapper;

    public JobInstanceServiceImpl(JobRepository jobRepository,
                                  JobInstanceRepository jobInstanceRepository,
                                  FlinkRemoteService flinkRemoteService,
                                  JobExecutionPlanner jobExecutionPlanner,
                                  FlinkRuntimeConfigParser flinkRuntimeConfigParser,
                                  JobExecutorRegistry jobExecutorRegistry,
                                  AirflowRemoteLogService airflowRemoteLogService,
                                  RustFsLogService rustFsLogService,
                                  SparkApplicationOperatorService sparkApplicationOperatorService,
                                  ScriptRuntimeProperties scriptRuntimeProperties,
                                  WorkflowInstanceRepository workflowInstanceRepository,
                                  WorkflowNodeRepository workflowNodeRepository,
                                  ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobInstanceRepository = jobInstanceRepository;
        this.flinkRemoteService = flinkRemoteService;
        this.jobExecutionPlanner = jobExecutionPlanner;
        this.flinkRuntimeConfigParser = flinkRuntimeConfigParser;
        this.jobExecutorRegistry = jobExecutorRegistry;
        this.airflowRemoteLogService = airflowRemoteLogService;
        this.rustFsLogService = rustFsLogService;
        this.sparkApplicationOperatorService = sparkApplicationOperatorService;
        this.scriptRuntimeProperties = scriptRuntimeProperties;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 手动执行作业，生成一个实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO execute(String jobId) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));

        return executeJob(job, null);
    }

    /**
     * 临时执行作业，不生成正式实例
     */
    @Override
    public JobInstanceBO executePreview(JobPreviewExecuteCmd cmd, String createdBy) {
        Assert.notNull(cmd, new SilentException("临时执行参数不能为空"));

        Job previewJob = new Job()
                .setName(cmd.getName() == null || cmd.getName().isBlank() ? "临时任务" : cmd.getName())
                .setEngineType(cmd.getEngineType())
                .setNodeType(cmd.getNodeType())
                .setContent(cmd.getContent())
                .setConfigJson(cmd.getConfigJson());
        String executableSql = jobExecutionPlanner.buildExecutableSql(previewJob);

        long startTime = System.currentTimeMillis();
        JobInstanceBO result = new JobInstanceBO()
                .setJobId("")
                .setJobName(previewJob.getName())
                .setEngineType(previewJob.getEngineType())
                .setContent(executableSql)
                .setStatus(ExecutionStatus.RUNNING)
                .setCreatedBy(createdBy)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedBy(createdBy)
                .setUpdatedAt(LocalDateTime.now());
        try {
            Job executeJob = new Job()
                    .setName(previewJob.getName())
                    .setEngineType(previewJob.getEngineType())
                    .setNodeType(previewJob.getNodeType())
                    .setContent(executableSql)
                    .setConfigJson(previewJob.getConfigJson());
            String resultData = jobExecutorRegistry.get(previewJob.getNodeType())
                    .execute(executeJob, new JobInstance().setId("preview"))
                    .getResultData();
            result.setStatus(ExecutionStatus.SUCCESS)
                    .setResultData(resultData)
                    .setCostTimeMs(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            result.setStatus(ExecutionStatus.FAILED)
                    .setErrorMessage(e.getMessage())
                    .setCostTimeMs(System.currentTimeMillis() - startTime);
        }
        return result;
    }

    /**
     * 启动正式Application Mode作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO startApplication(String jobId, String createdBy) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        Assert.isTrue(job.getStatus() == com.cyan.dataworks.enums.TaskStatus.ONLINE, new SilentException("只有已发布作业可启动正式任务"));
        Assert.isTrue(job.getEngineType() == EngineType.FLINK, new SilentException("当前仅Flink任务支持Application Mode启动"));

        cleanupOldFlinkInstances(job);
        String executableSql = jobExecutionPlanner.buildExecutableSql(job);
        JobInstanceCmd cmd = new JobInstanceCmd()
                .setJobId(jobId)
                .setJobName(job.getName())
                .setEngineType(job.getEngineType())
                .setContent(executableSql)
                .setStatus(ExecutionStatus.RUNNING);
        JobInstance instance = JobInstanceAppConvert.INSTANCE.toJobInstance(cmd);
        instance.setCreatedBy(createdBy);
        instance.setUpdatedBy(createdBy);
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instance = instance.save(jobInstanceRepository);

        long startTime = System.currentTimeMillis();
        try {
            FlinkRuntimeConfig runtimeConfig = flinkRuntimeConfigParser.parse(job);
            String resultData = flinkRemoteService.submitApplication(job.getId(), job.getName(), executableSql, runtimeConfig);
            bindApplicationInfo(instance, resultData);
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

        Job job = jobRepository.findById(original.getJobId());
        Assert.notNull(job, new SilentException("原作业不存在"));
        return executeJob(job, null);
    }

    /**
     * 调度器触发执行作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO runByScheduler(String jobId, JobRunBySchedulerCmd cmd) {
        Assert.notNull(cmd, new SilentException("调度器执行参数不能为空"));
        JobInstance existing = jobInstanceRepository.findBySchedulerTrace(cmd.getDagRunId(), cmd.getTaskId(), cmd.getTryNumber());
        if (existing != null) {
            return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(existing);
        }
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        return executeJob(job, cmd);
    }

    /**
     * 创建实例并执行作业
     */
    private JobInstanceBO executeJob(Job job, JobRunBySchedulerCmd schedulerCmd) {
        String snapshotContent = job.getNodeType() != null && job.getNodeType().isSqlNode()
                ? jobExecutionPlanner.buildExecutableSql(job)
                : job.getContent();
        JobInstanceCmd cmd = new JobInstanceCmd()
                .setJobId(job.getId())
                .setJobName(job.getName())
                .setEngineType(job.getEngineType())
                .setContent(snapshotContent)
                .setStatus(ExecutionStatus.RUNNING);
        if (schedulerCmd != null) {
            cmd.setSchedulerType(schedulerCmd.getSchedulerType())
                    .setSchedulerDagId(schedulerCmd.getDagId())
                    .setSchedulerDagRunId(schedulerCmd.getDagRunId())
                    .setSchedulerTaskId(schedulerCmd.getTaskId())
                    .setSchedulerTryNumber(schedulerCmd.getTryNumber());
        }
        JobInstance instance = JobInstanceAppConvert.INSTANCE.toJobInstance(cmd);
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instance = instance.save(jobInstanceRepository);
        long startTime = System.currentTimeMillis();
        try {
            Job executeJob = new Job()
                    .setId(job.getId())
                    .setName(job.getName())
                    .setDescription(job.getDescription())
                    .setEngineType(job.getEngineType())
                    .setNodeType(job.getNodeType())
                    .setContent(snapshotContent)
                    .setConfigJson(job.getConfigJson())
                    .setStatus(job.getStatus());
            JobExecutionResult result = jobExecutorRegistry.get(job.getNodeType()).execute(executeJob, instance);
            if (Boolean.TRUE.equals(result.getAsyncSubmitted())) {
                instance = bindAsyncRuntime(instance, result);
            } else {
                instance.markSuccess(result.getResultData(), System.currentTimeMillis() - startTime, jobInstanceRepository);
            }
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
        if (instance.getEngineType() == EngineType.SPARK) {
            sparkApplicationOperatorService.delete(
                    resolveRuntimeApplicationName(instance),
                    instance.getApplicationNamespace(),
                    instance.getConfigMapName());
        }
        instance.terminate(jobInstanceRepository);
        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
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

    /**
     * 查询调度器等待状态
     */
    @Override
    public JobInstanceBO findSchedulerStatus(String id) {
        JobInstance instance = jobInstanceRepository.findById(id);
        Assert.notNull(instance, new SilentException("实例不存在"));
        instance = syncSparkApplicationStatus(instance);
        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }

    /**
     * Pod执行完成回调
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO callback(String id, JobInstanceCallbackCmd cmd, String callbackToken) {
        Assert.notNull(cmd, new SilentException("实例回调参数不能为空"));
        String expectedToken = Optional.ofNullable(scriptRuntimeProperties.getCallbackToken()).orElse("");
        Assert.isTrue(expectedToken.isBlank() || expectedToken.equals(callbackToken), new SilentException("实例回调Token不合法"));
        JobInstance instance = jobInstanceRepository.findById(id);
        Assert.notNull(instance, new SilentException("实例不存在"));
        ExecutionStatus status = cmd.getStatus();
        Assert.isTrue(status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED, new SilentException("实例回调状态不合法"));
        if (status == ExecutionStatus.SUCCESS) {
            instance = instance.markCallbackSuccess(cmd.getResultData(), cmd.getLogObjectKey(), cmd.getStartedAt(), cmd.getFinishedAt(), LocalDateTime.now(), jobInstanceRepository);
        } else {
            String message = Optional.ofNullable(cmd.getErrorMessage()).filter(value -> !value.isBlank()).orElse("脚本任务执行失败，退出码：" + cmd.getExitCode());
            instance = instance.markCallbackFailed(message, cmd.getResultData(), cmd.getLogObjectKey(), cmd.getStartedAt(), cmd.getFinishedAt(), LocalDateTime.now(), jobInstanceRepository);
        }
        syncWorkflowInstanceState(instance);
        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }

    /**
     * 查询实例日志
     */
    @Override
    public JobInstanceLogBO getLogs(String id, JobInstanceLogQuery query) {
        JobInstance instance = jobInstanceRepository.findById(id);
        Assert.notNull(instance, new SilentException("实例不存在"));
        if (instance.getEngineType() == EngineType.SPARK) {
            int tailLines = normalizeTailLines(Optional.ofNullable(query).map(JobInstanceLogQuery::getTailLines).orElse(null));
            String applicationName = resolveRuntimeApplicationName(instance);
            String logs = sparkApplicationOperatorService.readDriverLog(applicationName, instance.getApplicationNamespace(), tailLines);
            if (logs == null || logs.isBlank()) {
                logs = Optional.ofNullable(instance.getResultData())
                        .filter(value -> !value.isBlank())
                        .orElse(Optional.ofNullable(instance.getErrorMessage()).orElse(""));
            }
            return new JobInstanceLogBO()
                    .setInstanceId(instance.getId())
                    .setDeploymentName(applicationName)
                    .setNamespace(Optional.ofNullable(instance.getApplicationNamespace()).orElse(""))
                    .setRole(JobLogRole.ALL)
                    .setTailLines(tailLines)
                    .setPods(List.of())
                    .setLogs(logs)
                    .setMessage(logs == null || logs.isBlank() ? "暂无Spark Driver日志" : "Spark Driver日志读取成功");
        }
        if (airflowRemoteLogService.supports(instance)) {
            String logs = airflowRemoteLogService.readTaskLog(instance);
            return new JobInstanceLogBO()
                    .setInstanceId(instance.getId())
                    .setDeploymentName(airflowRemoteLogService.buildObjectKey(instance))
                    .setNamespace("rustfs")
                    .setRole(JobLogRole.ALL)
                    .setTailLines(null)
                    .setPods(List.of())
                    .setLogs(logs)
                    .setMessage("Airflow远程日志读取成功");
        }
        if (instance.getEngineType() == EngineType.SHELL || instance.getEngineType() == EngineType.PYTHON) {
            String logs = Optional.ofNullable(instance.getLogObjectKey())
                    .filter(value -> !value.isBlank())
                    .map(rustFsLogService::readScriptLog)
                    .orElseGet(() -> Optional.ofNullable(instance.getResultData())
                            .filter(value -> !value.isBlank())
                            .orElse(Optional.ofNullable(instance.getErrorMessage()).orElse("")));
            return new JobInstanceLogBO()
                    .setInstanceId(instance.getId())
                    .setDeploymentName(Optional.ofNullable(instance.getRuntimeJobName()).orElse(""))
                    .setNamespace("rustfs")
                    .setRole(JobLogRole.ALL)
                    .setTailLines(null)
                    .setPods(List.of())
                    .setLogs(logs)
                    .setMessage(logs == null || logs.isBlank() ? "暂无脚本输出" : "脚本输出读取成功");
        }
        Assert.isTrue(instance.getEngineType() == EngineType.FLINK, new SilentException("只有Flink实例支持查看K8s Pod日志"));

        JsonNode resultData = parseResultData(instance.getResultData());
        String deploymentName = Optional.ofNullable(instance.getApplicationName())
                .filter(value -> !value.isBlank())
                .orElse(resultData == null ? "" : resultData.path("deploymentName").asText(""));
        Assert.notBlank(deploymentName, new SilentException("实例未关联FlinkDeployment"));
        String namespace = Optional.ofNullable(instance.getApplicationNamespace())
                .filter(value -> !value.isBlank())
                .orElse(resultData == null ? "" : resultData.path("namespace").asText(""));
        JobLogRole role = Optional.ofNullable(query)
                .map(JobInstanceLogQuery::getRole)
                .orElse(JobLogRole.ALL);
        int tailLines = normalizeTailLines(Optional.ofNullable(query).map(JobInstanceLogQuery::getTailLines).orElse(null));
        boolean previous = Optional.ofNullable(query).map(JobInstanceLogQuery::getPrevious).orElse(false);

        List<FlinkPodLogBO> podLogs = flinkRemoteService.getApplicationPodLogs(deploymentName, namespace, role, tailLines, previous);
        List<JobInstanceLogBO.PodLogBO> pods = podLogs.stream()
                .map(podLog -> new JobInstanceLogBO.PodLogBO()
                        .setPodName(podLog.getPodName())
                        .setRole(podLog.getRole())
                        .setContainerName(podLog.getContainerName())
                        .setLog(podLog.getLog()))
                .toList();
        String logs = pods.stream()
                .map(pod -> "===== " + pod.getPodName() + " / " + pod.getRole().getDesc() + " / " + pod.getContainerName() + " =====\n"
                        + Optional.ofNullable(pod.getLog()).orElse(""))
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
        String message = pods.isEmpty() ? "未找到匹配的Flink Pod" : "日志读取成功";
        return new JobInstanceLogBO()
                .setInstanceId(instance.getId())
                .setDeploymentName(deploymentName)
                .setNamespace(namespace)
                .setRole(role)
                .setTailLines(tailLines)
                .setPods(pods)
                .setLogs(logs)
                .setMessage(message);
    }

    /**
     * 绑定异步运行资源
     */
    private JobInstance bindAsyncRuntime(JobInstance instance, JobExecutionResult result) {
        if (result.getApplicationName() != null && !result.getApplicationName().isBlank()) {
            return instance.bindSparkApplication(
                    result.getRuntimeJobName(),
                    result.getApplicationName(),
                    result.getApplicationNamespace(),
                    result.getConfigMapName(),
                    jobInstanceRepository);
        }
        return instance.bindRuntimeJob(result.getRuntimeJobName(), jobInstanceRepository);
    }

    /**
     * 同步SparkApplication状态
     */
    private JobInstance syncSparkApplicationStatus(JobInstance instance) {
        if (instance.getEngineType() != EngineType.SPARK || instance.getStatus() != ExecutionStatus.RUNNING) {
            return instance;
        }
        String applicationName = resolveRuntimeApplicationName(instance);
        if (applicationName == null || applicationName.isBlank()) {
            return instance;
        }
        SparkApplicationBO application = sparkApplicationOperatorService.getStatus(applicationName, instance.getApplicationNamespace());
        if (Boolean.TRUE.equals(application.getCompleted())) {
            String resultData = """
                    {"applicationName":"%s","namespace":"%s","state":"%s","driverPodName":"%s"}
                    """.formatted(
                    safeJson(application.getApplicationName()),
                    safeJson(application.getNamespace()),
                    safeJson(application.getState()),
                    safeJson(application.getDriverPodName())).trim();
            instance = instance.markSuccess(resultData, calculateCostTimeMs(instance), jobInstanceRepository);
            syncWorkflowInstanceState(instance);
            return instance;
        }
        if (Boolean.TRUE.equals(application.getFailed())) {
            String logs = sparkApplicationOperatorService.readDriverLog(applicationName, instance.getApplicationNamespace(), 2000);
            String message = Optional.ofNullable(application.getMessage())
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> Optional.ofNullable(logs)
                            .filter(value -> !value.isBlank())
                            .orElse("SparkApplication执行失败：" + application.getState()));
            instance = instance.markFailed(message, calculateCostTimeMs(instance), jobInstanceRepository);
            syncWorkflowInstanceState(instance);
            return instance;
        }
        return instance;
    }

    /**
     * 解析运行应用名称
     */
    private String resolveRuntimeApplicationName(JobInstance instance) {
        return Optional.ofNullable(instance.getApplicationName())
                .filter(value -> !value.isBlank())
                .orElse(instance.getRuntimeJobName());
    }

    /**
     * 计算实例耗时
     */
    private long calculateCostTimeMs(JobInstance instance) {
        if (instance.getCreatedAt() == null) {
            return 0L;
        }
        return java.time.Duration.between(instance.getCreatedAt(), LocalDateTime.now()).toMillis();
    }

    /**
     * 转义JSON字符串内容
     */
    private String safeJson(String value) {
        return Optional.ofNullable(value).orElse("").replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 解析执行结果JSON
     */
    private JsonNode parseResultData(String resultData) {
        if (resultData == null || resultData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(resultData);
        } catch (Exception e) {
            throw new SilentException("实例执行结果解析失败：" + e.getMessage());
        }
    }

    /**
     * 规整日志尾部行数
     */
    private int normalizeTailLines(Integer tailLines) {
        if (tailLines == null || tailLines <= 0) {
            return 500;
        }
        return Math.min(tailLines, 5000);
    }

    /**
     * 同步本地工作流实例状态
     */
    private void syncWorkflowInstanceState(JobInstance instance) {
        if (instance.getWorkflowInstanceId() == null || instance.getWorkflowInstanceId().isBlank()) {
            return;
        }
        WorkflowInstance workflowInstance = workflowInstanceRepository.findById(instance.getWorkflowInstanceId());
        if (workflowInstance == null || workflowInstance.getStatus() != ExecutionStatus.RUNNING) {
            return;
        }
        List<JobInstance> instances = jobInstanceRepository.listByWorkflowInstanceId(instance.getWorkflowInstanceId());
        if (instances.stream().anyMatch(item -> item.getStatus() == ExecutionStatus.FAILED)) {
            workflowInstance.markFailed(instance.getErrorMessage(), workflowInstanceRepository);
            return;
        }
        int expectedNodeCount = workflowNodeRepository.listByWorkflowId(workflowInstance.getWorkflowId()).size();
        if (expectedNodeCount > 0
                && instances.size() >= expectedNodeCount
                && instances.stream().allMatch(item -> item.getStatus() == ExecutionStatus.SUCCESS)) {
            workflowInstance.markSuccess(workflowInstanceRepository);
        }
    }

    /**
     * 清理旧Flink实例
     */
    private void cleanupOldFlinkInstances(Job job) {
        JobInstance latestInstance = jobInstanceRepository.findLatestByJobId(job.getId());
        if (latestInstance != null) {
            String applicationName = Optional.ofNullable(latestInstance.getApplicationName())
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> parseApplicationNameQuietly(latestInstance.getResultData()));
            String configMapName = Optional.ofNullable(latestInstance.getConfigMapName())
                    .filter(value -> !value.isBlank())
                    .orElse(applicationName == null || applicationName.isBlank() ? "" : applicationName + "-sql");
            if (applicationName != null && !applicationName.isBlank()) {
                flinkRemoteService.deleteApplication(applicationName, configMapName);
            }
        }
        jobInstanceRepository.deleteByJobId(job.getId());
    }

    /**
     * 绑定Application信息
     */
    private void bindApplicationInfo(JobInstance instance, String resultData) {
        JsonNode node = parseResultData(resultData);
        if (node == null) {
            return;
        }
        instance.bindFlinkApplication(
                node.path("deploymentName").asText(""),
                node.path("namespace").asText(""),
                node.path("configMapName").asText(""),
                node.path("jobManagerPodName").asText(""),
                stringify(node.get("taskManagerPodNames"))
        );
    }

    /**
     * 安静解析Application名称
     */
    private String parseApplicationNameQuietly(String resultData) {
        try {
            JsonNode node = parseResultData(resultData);
            return node == null ? "" : node.path("deploymentName").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * JSON节点序列化
     */
    private String stringify(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "[]";
        }
    }
}
