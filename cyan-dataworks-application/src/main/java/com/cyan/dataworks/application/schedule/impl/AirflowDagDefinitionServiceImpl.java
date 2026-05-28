package com.cyan.dataworks.application.schedule.impl;

import com.cyan.dataworks.application.schedule.AirflowDagDefinitionService;
import com.cyan.dataworks.application.schedule.bo.AirflowDagDefinitionBO;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.dependency.JobDependency;
import com.cyan.dataworks.domain.job.dependency.repository.JobDependencyRepository;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.dataworks.infra.config.AirflowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Airflow DAG定义应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class AirflowDagDefinitionServiceImpl implements AirflowDagDefinitionService {

    /**
     * 调度配置仓储
     */
    private final JobScheduleRepository jobScheduleRepository;

    /**
     * 作业仓储
     */
    private final JobRepository jobRepository;

    /**
     * 作业依赖仓储
     */
    private final JobDependencyRepository jobDependencyRepository;

    /**
     * Airflow配置
     */
    private final AirflowProperties airflowProperties;

    public AirflowDagDefinitionServiceImpl(JobScheduleRepository jobScheduleRepository,
                                           JobRepository jobRepository,
                                           JobDependencyRepository jobDependencyRepository,
                                           AirflowProperties airflowProperties) {
        this.jobScheduleRepository = jobScheduleRepository;
        this.jobRepository = jobRepository;
        this.jobDependencyRepository = jobDependencyRepository;
        this.airflowProperties = airflowProperties;
    }

    /**
     * 查询启用的DAG定义
     */
    @Override
    public List<AirflowDagDefinitionBO> listEnabledDefinitions() {
        String prefix = Optional.ofNullable(airflowProperties.getDagPrefix()).filter(value -> !value.isBlank()).orElse("dataworks");
        List<JobSchedule> schedules = jobScheduleRepository.listEnabled();
        List<JobDependency> dependencies = jobDependencyRepository.listAll();
        List<AirflowDagDefinitionBO> definitions = schedules.stream()
                .map(schedule -> buildDefinition(prefix, schedule, dependencies))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        log.info("Airflow查询DAG定义: enabledScheduleCount={}, returnedDagCount={}, dagPrefix={}",
                schedules.size(), definitions.size(), prefix);
        return definitions;
    }

    /**
     * 构建作业DAG定义
     *
     * @param prefix       DAG前缀
     * @param schedule     调度配置
     * @param dependencies 作业依赖列表
     * @return DAG定义
     */
    private Optional<AirflowDagDefinitionBO> buildDefinition(String prefix,
                                                             JobSchedule schedule,
                                                             List<JobDependency> dependencies) {
        Job job = jobRepository.findById(schedule.getJobId());
        if (job == null) {
            log.warn("Airflow DAG定义跳过，调度配置关联作业不存在: scheduleId={}, jobId={}, cronExpression={}",
                    schedule.getId(), schedule.getJobId(), schedule.getCronExpression());
            return Optional.empty();
        }
        if (job.getStatus() != TaskStatus.ONLINE) {
            log.info("Airflow DAG定义跳过，作业未发布: jobId={}, name={}, status={}, cronExpression={}",
                    job.getId(), job.getName(), job.getStatus(), schedule.getCronExpression());
            return Optional.empty();
        }
        Map<String, List<JobDependency>> dependencyMap = Optional.ofNullable(dependencies).orElse(List.of())
                .stream()
                .collect(Collectors.groupingBy(
                        JobDependency::getDownstreamJobId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        try {
            LinkedHashMap<String, Job> taskJobs = new LinkedHashMap<>();
            collectTaskJobs(job.getId(), dependencyMap, taskJobs, new LinkedHashSet<>(), new LinkedHashSet<>());
            List<AirflowDagDefinitionBO.TaskBO> tasks = taskJobs.values().stream()
                    .map(taskJob -> buildTask(taskJob, taskJobs.keySet(), dependencyMap))
                    .toList();
            log.info("Airflow DAG定义生成: dagId={}, targetJobId={}, jobName={}, cronExpression={}, taskCount={}",
                    prefix + "_job_" + job.getId(), job.getId(), job.getName(), schedule.getCronExpression(), tasks.size());
            return Optional.of(new AirflowDagDefinitionBO()
                    .setDagId(prefix + "_job_" + job.getId())
                    .setJobId(job.getId())
                    .setJobName(job.getName())
                    .setCronExpression(schedule.getCronExpression())
                    .setTasks(tasks));
        } catch (Exception e) {
            log.warn("Airflow DAG定义跳过，依赖链无效: targetJobId={}, jobName={}, reason={}",
                    job.getId(), job.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 递归收集DAG任务作业
     */
    private void collectTaskJobs(String jobId,
                                 Map<String, List<JobDependency>> dependencyMap,
                                 LinkedHashMap<String, Job> taskJobs,
                                 Set<String> visiting,
                                 Set<String> visited) {
        if (visited.contains(jobId)) {
            return;
        }
        if (!visiting.add(jobId)) {
            throw new IllegalStateException("依赖关系成环: " + jobId);
        }
        Job job = jobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalStateException("依赖作业不存在: " + jobId);
        }
        if (job.getStatus() != TaskStatus.ONLINE) {
            throw new IllegalStateException("依赖作业未发布: " + jobId);
        }
        for (JobDependency dependency : dependencyMap.getOrDefault(jobId, List.of())) {
            collectTaskJobs(dependency.getUpstreamJobId(), dependencyMap, taskJobs, visiting, visited);
        }
        visiting.remove(jobId);
        visited.add(jobId);
        taskJobs.putIfAbsent(jobId, job);
    }

    /**
     * 构建Airflow任务定义
     */
    private AirflowDagDefinitionBO.TaskBO buildTask(Job job,
                                                    Set<String> taskJobIds,
                                                    Map<String, List<JobDependency>> dependencyMap) {
        List<String> upstreamTaskIds = new ArrayList<>();
        for (JobDependency dependency : dependencyMap.getOrDefault(job.getId(), List.of())) {
            if (taskJobIds.contains(dependency.getUpstreamJobId())) {
                upstreamTaskIds.add(taskId(dependency.getUpstreamJobId()));
            }
        }
        return new AirflowDagDefinitionBO.TaskBO()
                .setTaskId(taskId(job.getId()))
                .setJobId(job.getId())
                .setJobName(job.getName())
                .setEngineType(job.getEngineType())
                .setNodeType(job.getNodeType())
                .setUpstreamTaskIds(upstreamTaskIds);
    }

    /**
     * 构建Airflow任务ID
     */
    private String taskId(String jobId) {
        return "job_" + jobId;
    }
}
