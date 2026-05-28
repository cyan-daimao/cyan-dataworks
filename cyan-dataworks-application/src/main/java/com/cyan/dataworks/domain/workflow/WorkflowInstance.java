package com.cyan.dataworks.domain.workflow;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.workflow.repository.WorkflowInstanceRepository;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.WorkflowTriggerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流实例领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowInstance {

    /**
     * 主键
     */
    private String id;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * DAG运行ID
     */
    private String dagRunId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 触发类型
     */
    private WorkflowTriggerType triggerType;

    /**
     * 耗时（毫秒）
     */
    private Long costTimeMs;

    /**
     * 错误信息
     */
    private String errorMessage;

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
     * 保存工作流实例
     */
    public WorkflowInstance save(WorkflowInstanceRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        validateDefinition();
        return repository.save(this);
    }

    /**
     * 更新工作流实例
     */
    public WorkflowInstance update(WorkflowInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        return repository.updateById(this);
    }

    /**
     * 校验实例定义
     */
    public void validateDefinition() {
        Assert.notBlank(this.workflowId, new SilentException("工作流ID不能为空"));
        Assert.notBlank(this.dagId, new SilentException("DAG ID不能为空"));
        Assert.notBlank(this.dagRunId, new SilentException("DAG Run ID不能为空"));
        if (this.status == null) {
            this.status = ExecutionStatus.RUNNING;
        }
        if (this.triggerType == null) {
            this.triggerType = WorkflowTriggerType.MANUAL;
        }
    }

    /**
     * 标记工作流实例成功
     */
    public WorkflowInstance markSuccess(WorkflowInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("工作流实例ID不能为空"));
        this.status = ExecutionStatus.SUCCESS;
        this.updatedAt = LocalDateTime.now();
        return repository.updateById(this);
    }

    /**
     * 标记工作流实例失败
     */
    public WorkflowInstance markFailed(String errorMessage, WorkflowInstanceRepository repository) {
        Assert.notBlank(this.id, new SilentException("工作流实例ID不能为空"));
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
        return repository.updateById(this);
    }
}
