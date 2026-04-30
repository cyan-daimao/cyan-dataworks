package com.cyan.dataworks.domain.schedule;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.schedule.repository.ScheduleConfigRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 调度配置领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ScheduleConfig {

    /**
     * 主键
     */
    private String id;

    /**
     * 任务ID
     */
    private String taskId;

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
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 保存调度配置
     */
    public ScheduleConfig save(ScheduleConfigRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.notBlank(this.taskId, new SilentException("任务ID不能为空"));
        Assert.notBlank(this.cronExpression, new SilentException("Cron表达式不能为空"));
        if (this.enabled == null) {
            this.enabled = false;
        }
        return repository.save(this);
    }

    /**
     * 更新调度配置
     */
    public ScheduleConfig update(ScheduleConfigRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        Assert.notBlank(this.taskId, new SilentException("任务ID不能为空"));
        Assert.notBlank(this.cronExpression, new SilentException("Cron表达式不能为空"));
        return repository.updateById(this);
    }
}
