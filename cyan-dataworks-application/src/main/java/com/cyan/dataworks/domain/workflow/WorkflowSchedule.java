package com.cyan.dataworks.domain.workflow;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.workflow.repository.WorkflowScheduleRepository;
import com.cyan.dataworks.enums.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流调度配置领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowSchedule {

    /**
     * 主键
     */
    private String id;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 调度器类型
     */
    private SchedulerType schedulerType;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecuteTime;

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
     * 保存调度配置
     */
    public WorkflowSchedule save(WorkflowScheduleRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        validateDefinition();
        return repository.save(this);
    }

    /**
     * 更新调度配置
     */
    public WorkflowSchedule update(WorkflowScheduleRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        validateDefinition();
        return repository.updateById(this);
    }

    /**
     * 校验调度配置
     */
    public void validateDefinition() {
        Assert.notBlank(this.workflowId, new SilentException("工作流ID不能为空"));
        Assert.notBlank(this.cronExpression, new SilentException("Cron表达式不能为空"));
        if (this.enabled == null) {
            this.enabled = false;
        }
        if (this.schedulerType == null) {
            this.schedulerType = SchedulerType.AIRFLOW;
        }
    }
}
