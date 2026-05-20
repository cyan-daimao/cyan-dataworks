package com.cyan.dataworks.domain.job.schedule;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job.schedule.repository.JobScheduleRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 作业调度配置领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobSchedule {

    /**
     * 主键
     */
    private String id;

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 是否启用
     */
    private Boolean enabled;

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
    public JobSchedule save(JobScheduleRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.notBlank(this.jobId, new SilentException("作业ID不能为空"));
        Assert.notBlank(this.cronExpression, new SilentException("Cron表达式不能为空"));
        if (this.enabled == null) {
            this.enabled = false;
        }
        return repository.save(this);
    }

    /**
     * 更新调度配置
     */
    public JobSchedule update(JobScheduleRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        Assert.notBlank(this.jobId, new SilentException("作业ID不能为空"));
        Assert.notBlank(this.cronExpression, new SilentException("Cron表达式不能为空"));
        return repository.updateById(this);
    }
}
