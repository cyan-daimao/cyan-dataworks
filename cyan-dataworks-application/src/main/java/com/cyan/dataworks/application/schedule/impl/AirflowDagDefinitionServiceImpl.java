package com.cyan.dataworks.application.schedule.impl;

import com.cyan.dataworks.application.schedule.AirflowDagDefinitionService;
import com.cyan.dataworks.application.schedule.bo.AirflowDagDefinitionBO;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import com.cyan.dataworks.infra.config.AirflowProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Airflow DAG定义应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
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
     * Airflow配置
     */
    private final AirflowProperties airflowProperties;

    public AirflowDagDefinitionServiceImpl(JobScheduleRepository jobScheduleRepository,
                                           JobRepository jobRepository,
                                           AirflowProperties airflowProperties) {
        this.jobScheduleRepository = jobScheduleRepository;
        this.jobRepository = jobRepository;
        this.airflowProperties = airflowProperties;
    }

    /**
     * 查询启用的DAG定义
     */
    @Override
    public List<AirflowDagDefinitionBO> listEnabledDefinitions() {
        String prefix = Optional.ofNullable(airflowProperties.getDagPrefix()).filter(value -> !value.isBlank()).orElse("dataworks");
        return jobScheduleRepository.listEnabled().stream()
                .map(schedule -> buildDefinition(prefix, schedule))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * 构建单作业DAG定义
     *
     * @param prefix   DAG前缀
     * @param schedule 调度配置
     * @return DAG定义
     */
    private Optional<AirflowDagDefinitionBO> buildDefinition(String prefix, JobSchedule schedule) {
        Job job = jobRepository.findById(schedule.getJobId());
        if (job == null) {
            return Optional.empty();
        }
        String taskId = "job_" + job.getId();
        return Optional.of(new AirflowDagDefinitionBO()
                .setDagId(prefix + "_job_" + job.getId())
                .setJobId(job.getId())
                .setJobName(job.getName())
                .setCronExpression(schedule.getCronExpression())
                .setTasks(List.of(new AirflowDagDefinitionBO.TaskBO()
                        .setTaskId(taskId)
                        .setJobId(job.getId())
                        .setJobName(job.getName())
                        .setEngineType(job.getEngineType())
                        .setNodeType(job.getNodeType())
                        .setUpstreamTaskIds(List.of()))));
    }
}
