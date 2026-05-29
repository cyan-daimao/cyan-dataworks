package com.cyan.dataworks.domain.workflow;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.workflow.repository.WorkflowRepository;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工工作流领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Workflow {

    /**
     * 主键
     */
    private String id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * Airflow DAG ID
     */
    private String dagId;

    /**
     * 工作流状态
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
     * 保存工作流
     */
    public Workflow save(WorkflowRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        validateDefinition();
        if (this.status == null) {
            this.status = TaskStatus.DRAFT;
        }
        return repository.save(this);
    }

    /**
     * 更新工作流
     */
    public Workflow update(WorkflowRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        validateDefinition();
        return repository.updateById(this);
    }

    /**
     * 发布工作流
     */
    public Workflow publish(WorkflowRepository repository) {
        Assert.notBlank(this.id, new SilentException("发布时id不能为空"));
        validateDefinition();
        this.status = TaskStatus.ONLINE;
        return repository.updateById(this);
    }

    /**
     * 下线工作流
     */
    public Workflow offline(WorkflowRepository repository) {
        Assert.notBlank(this.id, new SilentException("下线时id不能为空"));
        Assert.isTrue(this.status == TaskStatus.ONLINE, new SilentException("只有已发布工作流可下线"));
        this.status = TaskStatus.OFFLINE;
        return repository.updateById(this);
    }

    /**
     * 删除工作流
     */
    public void delete(WorkflowRepository repository) {
        Assert.notBlank(this.id, new SilentException("删除时id不能为空"));
        repository.deleteById(this.id);
    }

    /**
     * 校验工作流定义
     */
    public void validateDefinition() {
        Assert.notBlank(this.name, new SilentException("工作流名称不能为空"));
    }
}
