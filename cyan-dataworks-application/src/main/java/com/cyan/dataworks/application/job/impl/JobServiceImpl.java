package com.cyan.dataworks.application.job.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataworks.application.job.JobService;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.application.job.convert.JobAppConvert;
import com.cyan.dataworks.application.job.dependency.JobDependencyService;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.application.job.runtime.FlinkRuntimeConfig;
import com.cyan.dataworks.application.job.runtime.FlinkRuntimeConfigParser;
import com.cyan.dataworks.application.job.lineage.JobLineageSyncService;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.query.JobPageQuery;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.infra.remote.flink.FlinkRemoteService;
import com.cyan.dataworks.infra.remote.flink.operator.FlinkApplicationOperatorService;
import com.cyan.dataworks.infra.schedule.ScheduleJobExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobScheduleRepository jobScheduleRepository;
    private final ScheduleJobExecutor scheduleJobExecutor;
    private final JobInstanceRepository jobInstanceRepository;
    private final FlinkRemoteService flinkRemoteService;
    private final FlinkApplicationOperatorService flinkApplicationOperatorService;
    private final JobExecutionPlanner jobExecutionPlanner;
    private final FlinkRuntimeConfigParser flinkRuntimeConfigParser;
    private final JobLineageSyncService jobLineageSyncService;
    private final JobDependencyService jobDependencyService;
    private final ObjectMapper objectMapper;

    public JobServiceImpl(JobRepository jobRepository,
                          JobScheduleRepository jobScheduleRepository,
                          ScheduleJobExecutor scheduleJobExecutor,
                          JobInstanceRepository jobInstanceRepository,
                          FlinkRemoteService flinkRemoteService,
                          FlinkApplicationOperatorService flinkApplicationOperatorService,
                          JobExecutionPlanner jobExecutionPlanner,
                          FlinkRuntimeConfigParser flinkRuntimeConfigParser,
                          JobLineageSyncService jobLineageSyncService,
                          JobDependencyService jobDependencyService,
                          ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobScheduleRepository = jobScheduleRepository;
        this.scheduleJobExecutor = scheduleJobExecutor;
        this.jobInstanceRepository = jobInstanceRepository;
        this.flinkRemoteService = flinkRemoteService;
        this.flinkApplicationOperatorService = flinkApplicationOperatorService;
        this.jobExecutionPlanner = jobExecutionPlanner;
        this.flinkRuntimeConfigParser = flinkRuntimeConfigParser;
        this.jobLineageSyncService = jobLineageSyncService;
        this.jobDependencyService = jobDependencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询作业
     */
    @Override
    public Page<JobBO> page(JobPageQuery query) {
        Page<Job> page = jobRepository.page(query);
        List<JobBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(JobAppConvert.INSTANCE::toJobBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 列表查询作业
     */
    @Override
    public List<JobBO> list(JobPageQuery query) {
        List<Job> jobs = jobRepository.list(query);
        return Optional.ofNullable(jobs).orElse(List.of())
                .stream().map(JobAppConvert.INSTANCE::toJobBO).toList();
    }

    /**
     * 根据ID查询作业
     */
    @Override
    public JobBO findById(String id) {
        Job job = jobRepository.findById(id);
        Assert.notNull(job, new SilentException("作业不存在"));
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * 保存作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobBO save(JobCmd cmd, String createdBy) {
        Job job = JobAppConvert.INSTANCE.toJob(cmd);
        job.setCreatedBy(createdBy);
        job.setUpdatedBy(createdBy);
        job = job.save(jobRepository);
        jobLineageSyncService.sync(job);
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * 更新作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobBO update(String id, JobCmd cmd, String updatedBy) {
        Job existing = jobRepository.findById(id);
        Assert.notNull(existing, new SilentException("作业不存在"));
        Job job = JobAppConvert.INSTANCE.toJob(cmd);
        job.setId(id);
        job.setCreatedBy(existing.getCreatedBy());
        job.setCreatedAt(existing.getCreatedAt());
        job.setUpdatedBy(updatedBy);
        job = job.update(jobRepository);
        jobLineageSyncService.sync(job);
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * 删除作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Job existing = jobRepository.findById(id);
        Assert.notNull(existing, new SilentException("作业不存在"));
        deleteFlinkApplicationIfNeeded(existing);
        jobDependencyService.deleteByJobId(id);
        existing.delete(jobRepository);
        jobScheduleRepository.deleteByJobId(id);
        scheduleJobExecutor.cancel(id);
    }

    /**
     * 发布作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobBO publish(String id, String updatedBy) {
        Job existing = jobRepository.findById(id);
        Assert.notNull(existing, new SilentException("作业不存在"));
        log.info("准备发布DataWorks作业: jobId={}, name={}, engineType={}, nodeType={}, status={}, updatedBy={}",
                existing.getId(), existing.getName(), existing.getEngineType(), existing.getNodeType(), existing.getStatus(), updatedBy);
        validateShellAirflowSchedule(existing);
        existing.setUpdatedBy(updatedBy);
        Job job = existing.publish(jobRepository);
        jobLineageSyncService.sync(job);
        syncFlinkApplicationIfNeeded(job);
        log.info("DataWorks作业发布完成: jobId={}, name={}, engineType={}, nodeType={}, status={}",
                job.getId(), job.getName(), job.getEngineType(), job.getNodeType(), job.getStatus());
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * Shell作业发布到生产前必须具备启用的Airflow调度，否则Airflow不会生成DAG。
     */
    private void validateShellAirflowSchedule(Job job) {
        if (job.getEngineType() != EngineType.SHELL) {
            return;
        }
        JobSchedule schedule = jobScheduleRepository.findByJobId(job.getId());
        log.info("校验Shell作业Airflow调度配置: jobId={}, scheduleExists={}, enabled={}, schedulerType={}, cronExpression={}",
                job.getId(),
                schedule != null,
                schedule == null ? null : schedule.getEnabled(),
                schedule == null ? null : schedule.getSchedulerType(),
                schedule == null ? null : schedule.getCronExpression());
        boolean validAirflowSchedule = schedule != null
                && Boolean.TRUE.equals(schedule.getEnabled())
                && schedule.getSchedulerType() == SchedulerType.AIRFLOW
                && schedule.getCronExpression() != null
                && !schedule.getCronExpression().isBlank();
        if (!validAirflowSchedule) {
            log.warn("Shell作业Airflow调度配置无效，拒绝发布: jobId={}, scheduleExists={}, enabled={}, schedulerType={}, cronExpression={}",
                    job.getId(),
                    schedule != null,
                    schedule == null ? null : schedule.getEnabled(),
                    schedule == null ? null : schedule.getSchedulerType(),
                    schedule == null ? null : schedule.getCronExpression());
        }
        Assert.isTrue(validAirflowSchedule, new SilentException("Shell作业发布前请填写Cron并启用Airflow调度"));
    }

    /**
     * Flink Job 发布时，若已有运行中的 Application，则删除旧资源并重新创建
     */
    private void syncFlinkApplicationIfNeeded(Job job) {
        if (job.getEngineType() != EngineType.FLINK) {
            return;
        }
        try {
            cleanupFlinkApplicationAndInstances(job);
            String executableSql = jobExecutionPlanner.buildExecutableSql(job);
            JobInstance instance = new JobInstance()
                    .setJobId(job.getId())
                    .setJobName(job.getName())
                    .setEngineType(job.getEngineType())
                    .setContent(executableSql)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setCreatedBy(job.getUpdatedBy())
                    .setUpdatedBy(job.getUpdatedBy())
                    .setCreatedAt(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now());
            instance = instance.save(jobInstanceRepository);
            long startTime = System.currentTimeMillis();
            try {
                FlinkRuntimeConfig runtimeConfig = flinkRuntimeConfigParser.parse(job);
                String resultData = flinkRemoteService.submitApplication(job.getId(), job.getName(), executableSql, runtimeConfig);
                bindApplicationInfo(instance, resultData);
                instance.markSuccess(resultData, System.currentTimeMillis() - startTime, jobInstanceRepository);
                log.info("Flink Job {} 发布成功，已创建 K8s Application: {}", job.getId(), instance.getApplicationName());
            } catch (Exception e) {
                instance.markFailed(e.getMessage(), System.currentTimeMillis() - startTime, jobInstanceRepository);
                throw e;
            }
        } catch (Exception e) {
            log.error("Flink Job {} 同步 K8s Application 失败: {}", job.getId(), e.getMessage(), e);
            // 不抛异常，避免影响发布操作本身
        }
    }

    /**
     * 从 resultData JSON 中解析 deploymentName
     */
    private String parseDeploymentName(String resultData) {
        try {
            JsonNode node = objectMapper.readTree(resultData);
            return node.path("deploymentName").asText(null);
        } catch (Exception e) {
            log.warn("解析 resultData 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从实例中解析Deployment名称
     */
    private String parseDeploymentName(JobInstance instance) {
        if (instance == null) {
            return "";
        }
        return Optional.ofNullable(instance.getApplicationName())
                .filter(value -> !value.isBlank())
                .orElseGet(() -> parseDeploymentName(instance.getResultData()));
    }

    /**
     * 从实例中解析ConfigMap名称
     */
    private String parseConfigMapName(JobInstance instance, String deploymentName) {
        return Optional.ofNullable(instance)
                .map(JobInstance::getConfigMapName)
                .filter(value -> !value.isBlank())
                .orElse(deploymentName == null || deploymentName.isBlank() ? "" : deploymentName + "-sql");
    }

    /**
     * 清理Flink Application和历史实例
     */
    private void cleanupFlinkApplicationAndInstances(Job job) {
        JobInstance latestInstance = jobInstanceRepository.findLatestByJobId(job.getId());
        String deploymentName = parseDeploymentName(latestInstance);
        if (StrUtils.isNotBlank(deploymentName)) {
            String configMapName = parseConfigMapName(latestInstance, deploymentName);
            flinkApplicationOperatorService.delete(deploymentName, configMapName);
            log.info("Flink Job {} 发布前已清理旧 K8s Application: {}", job.getId(), deploymentName);
        }
        jobInstanceRepository.deleteByJobId(job.getId());
    }

    /**
     * 绑定Application信息
     */
    private void bindApplicationInfo(JobInstance instance, String resultData) {
        try {
            JsonNode node = objectMapper.readTree(resultData);
            instance.bindFlinkApplication(
                    node.path("deploymentName").asText(""),
                    node.path("namespace").asText(""),
                    node.path("configMapName").asText(""),
                    node.path("jobManagerPodName").asText(""),
                    stringify(node.get("taskManagerPodNames"))
            );
        } catch (Exception e) {
            log.warn("绑定Flink Application信息失败: {}", e.getMessage());
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

    /**
     * Flink Job 下线/删除时，清理对应的 K8s Application
     */
    private void deleteFlinkApplicationIfNeeded(Job job) {
        if (job.getEngineType() != EngineType.FLINK) {
            return;
        }
        try {
            JobInstance latestInstance = jobInstanceRepository.findLatestByJobId(job.getId());
            if (latestInstance == null || latestInstance.getResultData() == null) {
                return;
            }
            String deploymentName = parseDeploymentName(latestInstance);
            if (StrUtils.isBlank(deploymentName)) {
                return;
            }
            String configMapName = parseConfigMapName(latestInstance, deploymentName);
            flinkApplicationOperatorService.delete(deploymentName, configMapName);
            log.info("Flink Job {} 下线/删除，已清理 K8s Application: {}", job.getId(), deploymentName);
        } catch (Exception e) {
            log.error("Flink Job {} 清理 K8s Application 失败: {}", job.getId(), e.getMessage(), e);
            // 不抛异常，避免影响下线/删除操作本身
        }
    }

    /**
     * 下线作业
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobBO offline(String id, String updatedBy) {
        Job existing = jobRepository.findById(id);
        Assert.notNull(existing, new SilentException("作业不存在"));
        existing.setUpdatedBy(updatedBy);
        Job job = existing.offline(jobRepository);
        scheduleJobExecutor.cancel(id);
        deleteFlinkApplicationIfNeeded(job);
        return JobAppConvert.INSTANCE.toJobBO(job);
    }
}
