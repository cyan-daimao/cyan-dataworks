package com.cyan.dataworks.application.job.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.JobService;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.application.job.convert.JobAppConvert;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.query.JobPageQuery;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.infra.remote.flink.FlinkRemoteService;
import com.cyan.dataworks.infra.remote.flink.operator.FlinkApplicationOperatorService;
import com.cyan.dataworks.infra.schedule.ScheduleJobExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper objectMapper;

    public JobServiceImpl(JobRepository jobRepository,
                          JobScheduleRepository jobScheduleRepository,
                          ScheduleJobExecutor scheduleJobExecutor,
                          JobInstanceRepository jobInstanceRepository,
                          FlinkRemoteService flinkRemoteService,
                          FlinkApplicationOperatorService flinkApplicationOperatorService,
                          JobExecutionPlanner jobExecutionPlanner,
                          ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobScheduleRepository = jobScheduleRepository;
        this.scheduleJobExecutor = scheduleJobExecutor;
        this.jobInstanceRepository = jobInstanceRepository;
        this.flinkRemoteService = flinkRemoteService;
        this.flinkApplicationOperatorService = flinkApplicationOperatorService;
        this.jobExecutionPlanner = jobExecutionPlanner;
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
        existing.setUpdatedBy(updatedBy);
        Job job = existing.publish(jobRepository);
        syncFlinkApplicationIfNeeded(job);
        return JobAppConvert.INSTANCE.toJobBO(job);
    }

    /**
     * Flink Job 发布时，若已有运行中的 Application，则删除旧资源并重新创建
     */
    private void syncFlinkApplicationIfNeeded(Job job) {
        if (job.getEngineType() != EngineType.FLINK) {
            return;
        }
        try {
            JobInstance latestInstance = jobInstanceRepository.findLatestByJobId(job.getId());
            if (latestInstance == null || latestInstance.getResultData() == null) {
                return;
            }
            String deploymentName = parseDeploymentName(latestInstance.getResultData());
            if (deploymentName == null || deploymentName.isBlank()) {
                return;
            }
            // 确认 K8s 上仍存在该 Deployment
            if (flinkApplicationOperatorService.get(deploymentName) == null) {
                log.info("Flink Job {} 无运行中的 FlinkDeployment，跳过同步", job.getId());
                return;
            }
            String configMapName = deploymentName + "-sql";
            // 1. 删除旧 K8s 资源
            flinkApplicationOperatorService.delete(deploymentName, configMapName);
            // 2. 用最新 SQL 重新提交
            String executableSql = jobExecutionPlanner.buildExecutableSql(job);
            flinkRemoteService.submitApplication(job.getName(), executableSql);
            log.info("Flink Job {} 发布成功，已同步更新 K8s Application: {}", job.getId(), deploymentName);
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
            String deploymentName = parseDeploymentName(latestInstance.getResultData());
            if (deploymentName == null || deploymentName.isBlank()) {
                return;
            }
            String configMapName = deploymentName + "-sql";
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
