package com.cyan.dataworks.application.workflow.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.runtime.JobExecutionPlanner;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.cmd.JobInstanceCmd;
import com.cyan.dataworks.application.job_instance.convert.JobInstanceAppConvert;
import com.cyan.dataworks.application.job_instance.executor.JobExecutionResult;
import com.cyan.dataworks.application.job_instance.executor.JobExecutorRegistry;
import com.cyan.dataworks.application.workflow.WorkflowRunService;
import com.cyan.dataworks.application.workflow.bo.AirflowDagRunBO;
import com.cyan.dataworks.application.workflow.bo.AirflowTaskInstanceBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowInstanceBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowRunBySchedulerCmd;
import com.cyan.dataworks.application.workflow.convert.WorkflowAppConvert;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.domain.workflow.Workflow;
import com.cyan.dataworks.domain.workflow.WorkflowInstance;
import com.cyan.dataworks.domain.workflow.WorkflowNode;
import com.cyan.dataworks.domain.workflow.query.WorkflowInstancePageQuery;
import com.cyan.dataworks.domain.workflow.repository.WorkflowInstanceRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowNodeRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowRepository;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.dataworks.enums.WorkflowTriggerType;
import com.cyan.dataworks.infra.remote.airflow.AirflowOrchestrationGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工作流运行应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class WorkflowRunServiceImpl implements WorkflowRunService {

    /** 工作流仓储 */
    private final WorkflowRepository workflowRepository;

    /** 工作流节点仓储 */
    private final WorkflowNodeRepository workflowNodeRepository;

    /** 工作流实例仓储 */
    private final WorkflowInstanceRepository workflowInstanceRepository;

    /** 作业仓储 */
    private final JobRepository jobRepository;

    /** 作业实例仓储 */
    private final JobInstanceRepository jobInstanceRepository;

    /** 作业执行规划器 */
    private final JobExecutionPlanner jobExecutionPlanner;

    /** 作业执行器注册表 */
    private final JobExecutorRegistry jobExecutorRegistry;

    /** Airflow编排网关 */
    private final AirflowOrchestrationGateway airflowGateway;

    public WorkflowRunServiceImpl(WorkflowRepository workflowRepository,
                                  WorkflowNodeRepository workflowNodeRepository,
                                  WorkflowInstanceRepository workflowInstanceRepository,
                                  JobRepository jobRepository,
                                  JobInstanceRepository jobInstanceRepository,
                                  JobExecutionPlanner jobExecutionPlanner,
                                  JobExecutorRegistry jobExecutorRegistry,
                                  AirflowOrchestrationGateway airflowGateway) {
        this.workflowRepository = workflowRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.jobRepository = jobRepository;
        this.jobInstanceRepository = jobInstanceRepository;
        this.jobExecutionPlanner = jobExecutionPlanner;
        this.jobExecutorRegistry = jobExecutorRegistry;
        this.airflowGateway = airflowGateway;
    }

    /** 触发工作流运行 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceBO trigger(String workflowId, String operator) {
        Workflow workflow = requireOnlineWorkflow(workflowId);
        AirflowDagRunBO dagRun = airflowGateway.triggerDagRun(workflow.getDagId());
        WorkflowInstance existing = workflowInstanceRepository.findByDagRun(workflow.getDagId(), dagRun.getDagRunId());
        if (existing != null) {
            return WorkflowAppConvert.INSTANCE.toInstanceBO(existing);
        }
        WorkflowInstance instance = new WorkflowInstance()
                .setWorkflowId(workflowId)
                .setWorkflowName(workflow.getName())
                .setDagId(workflow.getDagId())
                .setDagRunId(dagRun.getDagRunId())
                .setStatus(toExecutionStatus(dagRun.getState()))
                .setTriggerType(WorkflowTriggerType.MANUAL)
                .setCreatedBy(operator)
                .setUpdatedBy(operator)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .save(workflowInstanceRepository);
        return WorkflowAppConvert.INSTANCE.toInstanceBO(instance);
    }

    /** 分页查询本地工作流实例 */
    @Override
    public Page<WorkflowInstanceBO> pageInstances(WorkflowInstancePageQuery query) {
        Page<WorkflowInstance> page = workflowInstanceRepository.page(query);
        List<WorkflowInstanceBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(WorkflowAppConvert.INSTANCE::toInstanceBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /** 查询DAG Run列表 */
    @Override
    public List<AirflowDagRunBO> listDagRuns(String workflowId, Integer limit, Integer offset) {
        Workflow workflow = requireWorkflow(workflowId);
        return airflowGateway.listDagRuns(workflow.getDagId(), limit, offset);
    }

    /** 查询DAG Run详情 */
    @Override
    public AirflowDagRunBO getDagRun(String workflowId, String dagRunId) {
        Workflow workflow = requireWorkflow(workflowId);
        return airflowGateway.getDagRun(workflow.getDagId(), dagRunId);
    }

    /** 更新DAG Run状态 */
    @Override
    public AirflowDagRunBO updateDagRunState(String workflowId, String dagRunId, String state) {
        Workflow workflow = requireWorkflow(workflowId);
        AirflowDagRunBO result = airflowGateway.updateDagRunState(workflow.getDagId(), dagRunId, state);
        syncWorkflowInstanceState(workflow, result);
        return result;
    }

    /** 查询任务实例列表 */
    @Override
    public List<AirflowTaskInstanceBO> listTaskInstances(String workflowId, String dagRunId) {
        Workflow workflow = requireWorkflow(workflowId);
        return airflowGateway.listTaskInstances(workflow.getDagId(), dagRunId);
    }

    /** 重跑任务实例 */
    @Override
    public void rerunTaskInstance(String workflowId, String dagRunId, String taskId) {
        Workflow workflow = requireWorkflow(workflowId);
        airflowGateway.rerunTaskInstance(workflow.getDagId(), dagRunId, taskId);
    }

    /** 更新任务实例状态 */
    @Override
    public AirflowTaskInstanceBO updateTaskInstanceState(String workflowId, String dagRunId, String taskId, String state) {
        Workflow workflow = requireWorkflow(workflowId);
        return airflowGateway.updateTaskInstanceState(workflow.getDagId(), dagRunId, taskId, state);
    }

    /** 调度器触发节点执行 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobInstanceBO runNodeByScheduler(String workflowId, String nodeId, WorkflowRunBySchedulerCmd cmd) {
        Assert.notNull(cmd, new SilentException("调度器执行参数不能为空"));
        Workflow workflow = requireOnlineWorkflow(workflowId);
        WorkflowNode node = workflowNodeRepository.findById(nodeId);
        Assert.notNull(node, new SilentException("工作流节点不存在"));
        Assert.isTrue(workflowId.equals(node.getWorkflowId()), new SilentException("节点不属于当前工作流"));
        WorkflowInstance workflowInstance = ensureScheduledWorkflowInstance(workflow, cmd);
        JobInstance existing = jobInstanceRepository.findBySchedulerTrace(cmd.getDagRunId(), cmd.getTaskId(), cmd.getTryNumber());
        if (existing != null) {
            return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(existing);
        }
        Job job = jobRepository.findById(node.getJobId());
        Assert.notNull(job, new SilentException("节点关联作业不存在"));
        Assert.isTrue(job.getStatus() == TaskStatus.ONLINE, new SilentException("节点关联作业未发布"));
        return executeJob(workflowInstance, node, job, cmd);
    }

    private JobInstanceBO executeJob(WorkflowInstance workflowInstance,
                                     WorkflowNode node,
                                     Job job,
                                     WorkflowRunBySchedulerCmd schedulerCmd) {
        String snapshotContent = job.getNodeType() != null && job.getNodeType().isSqlNode()
                ? jobExecutionPlanner.buildExecutableSql(job)
                : job.getContent();
        JobInstanceCmd cmd = new JobInstanceCmd()
                .setWorkflowInstanceId(workflowInstance.getId())
                .setWorkflowNodeId(node.getId())
                .setJobId(job.getId())
                .setJobName(job.getName())
                .setEngineType(job.getEngineType())
                .setContent(snapshotContent)
                .setStatus(ExecutionStatus.RUNNING)
                .setSchedulerType(schedulerCmd.getSchedulerType())
                .setSchedulerDagId(schedulerCmd.getDagId())
                .setSchedulerDagRunId(schedulerCmd.getDagRunId())
                .setSchedulerTaskId(schedulerCmd.getTaskId())
                .setSchedulerTryNumber(schedulerCmd.getTryNumber());
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
                instance.bindRuntimeJob(result.getRuntimeJobName(), jobInstanceRepository);
            } else {
                instance.markSuccess(result.getResultData(), System.currentTimeMillis() - startTime, jobInstanceRepository);
            }
        } catch (Exception e) {
            instance.markFailed(e.getMessage(), System.currentTimeMillis() - startTime, jobInstanceRepository);
        }
        return JobInstanceAppConvert.INSTANCE.toJobInstanceBO(instance);
    }

    private WorkflowInstance ensureScheduledWorkflowInstance(Workflow workflow, WorkflowRunBySchedulerCmd cmd) {
        WorkflowInstance existing = workflowInstanceRepository.findByDagRun(cmd.getDagId(), cmd.getDagRunId());
        if (existing != null) {
            return existing;
        }
        return new WorkflowInstance()
                .setWorkflowId(workflow.getId())
                .setWorkflowName(workflow.getName())
                .setDagId(cmd.getDagId())
                .setDagRunId(cmd.getDagRunId())
                .setStatus(ExecutionStatus.RUNNING)
                .setTriggerType(WorkflowTriggerType.SCHEDULED)
                .setCreatedBy("airflow")
                .setUpdatedBy("airflow")
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .save(workflowInstanceRepository);
    }

    private void syncWorkflowInstanceState(Workflow workflow, AirflowDagRunBO dagRun) {
        WorkflowInstance instance = workflowInstanceRepository.findByDagRun(workflow.getDagId(), dagRun.getDagRunId());
        if (instance == null) {
            return;
        }
        instance.setStatus(toExecutionStatus(dagRun.getState()))
                .setUpdatedAt(LocalDateTime.now())
                .update(workflowInstanceRepository);
    }

    private Workflow requireWorkflow(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        return workflow;
    }

    private Workflow requireOnlineWorkflow(String workflowId) {
        Workflow workflow = requireWorkflow(workflowId);
        Assert.isTrue(workflow.getStatus() == TaskStatus.ONLINE, new SilentException("只有已发布工作流可运行"));
        Assert.notBlank(workflow.getDagId(), new SilentException("工作流DAG ID为空"));
        return workflow;
    }

    private ExecutionStatus toExecutionStatus(String state) {
        if ("success".equalsIgnoreCase(state)) {
            return ExecutionStatus.SUCCESS;
        }
        if ("failed".equalsIgnoreCase(state)) {
            return ExecutionStatus.FAILED;
        }
        return ExecutionStatus.RUNNING;
    }
}
