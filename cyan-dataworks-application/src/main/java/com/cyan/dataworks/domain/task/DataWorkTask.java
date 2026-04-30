package com.cyan.dataworks.domain.task;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.task.repository.DataWorkTaskRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工任务领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DataWorkTask {

    /**
     * 主键
     */
    private String id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * SQL内容
     */
    private String sqlContent;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 保存任务
     */
    public DataWorkTask save(DataWorkTaskRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.notBlank(this.name, new SilentException("任务名称不能为空"));
        Assert.notNull(this.engineType, new SilentException("引擎类型不能为空"));
        Assert.notBlank(this.sqlContent, new SilentException("SQL内容不能为空"));
        if (this.status == null) {
            this.status = TaskStatus.DRAFT;
        }
        return repository.save(this);
    }

    /**
     * 更新任务
     */
    public DataWorkTask update(DataWorkTaskRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        Assert.notBlank(this.name, new SilentException("任务名称不能为空"));
        Assert.notNull(this.engineType, new SilentException("引擎类型不能为空"));
        Assert.notBlank(this.sqlContent, new SilentException("SQL内容不能为空"));
        return repository.updateById(this);
    }

    /**
     * 删除任务
     */
    public void delete(DataWorkTaskRepository repository) {
        Assert.notBlank(this.id, new SilentException("删除时id不能为空"));
        repository.deleteById(this.id);
    }
}
