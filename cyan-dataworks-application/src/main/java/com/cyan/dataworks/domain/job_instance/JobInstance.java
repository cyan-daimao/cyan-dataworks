package com.cyan.dataworks.domain.job_instance;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job_instance.repository.JobInstanceRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业实例（JobInstance）领域对象
 *
 * <p>对标 Spark 中的 Task 实例概念：一个 JobInstance 代表一次具体的执行记录，
 * 包含执行状态、耗时、结果数据、错误信息等运行时数据。每个 JobInstance 都隶属于一个 Job。</p>
 *
 * <p>Flink 实时任务虽然理论上持续运行无明确"实例"边界，但为统一建模，
 * 每次手动触发或调度触发仍产生一个 JobInstance，用于记录执行快照。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstance {

    /**
     * 主键
     */
    private String id;

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 工作流实例ID
     */
    private String workflowInstanceId;

    /**
     * 工作流节点ID
     */
    private String workflowNodeId;

    /**
     * 作业名称（快照，防止 Job 改名后历史记录丢失名称）
     */
    private String jobName;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * 任务内容（快照，记录执行时的 SQL）
     */
    private String content;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 耗时（毫秒）
     */
    private Long costTimeMs;

    /**
     * 结果数据（JSON）
     */
    private String resultData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * Flink Application名称
     */
    private String applicationName;

    /**
     * Flink Application命名空间
     */
    private String applicationNamespace;

    /**
     * Flink ConfigMap名称
     */
    private String configMapName;

    /**
     * JobManager Pod名称
     */
    private String jobManagerPodName;

    /**
     * TaskManager Pod名称列表（JSON）
     */
    private String taskManagerPodNames;

    /**
     * 调度器类型
     */
    private SchedulerType schedulerType;

    /**
     * 调度器DAG ID
     */
    private String schedulerDagId;

    /**
     * 调度器DAG运行ID
     */
    private String schedulerDagRunId;

    /**
     * 调度器任务ID
     */
    private String schedulerTaskId;

    /**
     * 调度器重试次数
     */
    private Integer schedulerTryNumber;

    /**
     * 运行时Kubernetes Job名称
     */
    private String runtimeJobName;

    /**
     * RustFS日志对象Key
     */
    private String logObjectKey;

    /**
     * 运行开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 运行结束时间
     */
    private LocalDateTime finishedAt;

    /**
     * 回调时间
     */
    private LocalDateTime callbackAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 保存实例
     */
    public JobInstance save(JobInstanceRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.isTrue((this.jobId != null && !this.jobId.isBlank()) || (this.workflowNodeId != null && !this.workflowNodeId.isBlank()),
                new SilentException("作业ID和工作流节点ID不能同时为空"));
        Assert.notNull(this.engineType, new SilentException("引擎类型不能为空"));
        Assert.notBlank(this.content, new SilentException("任务内容不能为空"));
        Assert.notNull(this.status, new SilentException("执行状态不能为空"));
        return repository.save(this);
    }

    /**
     * 更新实例
     */
    public JobInstance update(JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        return repository.updateById(this);
    }

    /**
     * 标记实例为成功
     */
    public JobInstance markSuccess(String resultData, long costTimeMs, JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("实例id不能为空"));
        Assert.isTrue(this.status == ExecutionStatus.RUNNING, new SilentException("只有运行中实例可标记成功"));
        this.status = ExecutionStatus.SUCCESS;
        this.resultData = resultData;
        this.costTimeMs = costTimeMs;
        return repository.updateById(this);
    }

    /**
     * 标记实例为失败
     */
    public JobInstance markFailed(String errorMessage, long costTimeMs, JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("实例id不能为空"));
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.costTimeMs = costTimeMs;
        return repository.updateById(this);
    }

    /**
     * 绑定运行时任务信息
     */
    public JobInstance bindRuntimeJob(String runtimeJobName, JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("实例id不能为空"));
        this.runtimeJobName = runtimeJobName;
        return repository.updateById(this);
    }

    /**
     * 回调标记实例成功
     */
    public JobInstance markCallbackSuccess(String resultData,
                                           String logObjectKey,
                                           LocalDateTime startedAt,
                                           LocalDateTime finishedAt,
                                           LocalDateTime callbackAt,
                                           JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("实例id不能为空"));
        if (this.status == ExecutionStatus.SUCCESS) {
            return this;
        }
        Assert.isTrue(this.status != ExecutionStatus.FAILED, new SilentException("失败实例不能被回调覆盖为成功"));
        this.status = ExecutionStatus.SUCCESS;
        this.resultData = resultData;
        this.errorMessage = null;
        this.logObjectKey = logObjectKey;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.callbackAt = callbackAt == null ? LocalDateTime.now() : callbackAt;
        this.costTimeMs = calculateCostTimeMs(startedAt, finishedAt);
        return repository.updateById(this);
    }

    /**
     * 回调标记实例失败
     */
    public JobInstance markCallbackFailed(String errorMessage,
                                          String resultData,
                                          String logObjectKey,
                                          LocalDateTime startedAt,
                                          LocalDateTime finishedAt,
                                          LocalDateTime callbackAt,
                                          JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("实例id不能为空"));
        if (this.status == ExecutionStatus.FAILED) {
            return this;
        }
        Assert.isTrue(this.status != ExecutionStatus.SUCCESS, new SilentException("成功实例不能被回调覆盖为失败"));
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.resultData = resultData;
        this.logObjectKey = logObjectKey;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.callbackAt = callbackAt == null ? LocalDateTime.now() : callbackAt;
        this.costTimeMs = calculateCostTimeMs(startedAt, finishedAt);
        return repository.updateById(this);
    }

    /**
     * 终止实例
     */
    public JobInstance terminate(JobInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("实例id不能为空"));
        Assert.isTrue(this.status == ExecutionStatus.RUNNING, new SilentException("只有运行中实例可终止"));
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = "用户手动终止";
        return repository.updateById(this);
    }

    /**
     * 绑定Flink Application运行信息
     */
    public JobInstance bindFlinkApplication(String applicationName,
                                            String applicationNamespace,
                                            String configMapName,
                                            String jobManagerPodName,
                                            String taskManagerPodNames) {
        this.applicationName = applicationName;
        this.applicationNamespace = applicationNamespace;
        this.configMapName = configMapName;
        this.jobManagerPodName = jobManagerPodName;
        this.taskManagerPodNames = taskManagerPodNames;
        return this;
    }

    /**
     * 计算运行耗时
     */
    private Long calculateCostTimeMs(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return this.costTimeMs;
        }
        return java.time.Duration.between(startedAt, finishedAt).toMillis();
    }
}
