package com.cyan.dataworks.domain.workflow;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流级依赖领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDependency {

    /** 主键 */
    private String id;

    /** 上游工作流ID */
    private String upstreamWorkflowId;

    /** 下游工作流ID */
    private String downstreamWorkflowId;

    /** 依赖类型 */
    private JobDependencyType dependencyType;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 删除时间 */
    private LocalDateTime deletedAt;

    /**
     * 校验依赖定义
     */
    public void validateDefinition() {
        Assert.notBlank(upstreamWorkflowId, new SilentException("上游工作流ID不能为空"));
        Assert.notBlank(downstreamWorkflowId, new SilentException("下游工作流ID不能为空"));
        Assert.isTrue(!upstreamWorkflowId.equals(downstreamWorkflowId), new SilentException("工作流不能依赖自己"));
        if (dependencyType == null) {
            dependencyType = JobDependencyType.SCHEDULE_SAME_CYCLE;
        }
    }
}
