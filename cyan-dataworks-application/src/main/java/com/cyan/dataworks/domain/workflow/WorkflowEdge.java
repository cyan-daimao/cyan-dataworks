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
 * 工作流依赖边领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowEdge {

    /**
     * 主键
     */
    private String id;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 上游节点ID
     */
    private String upstreamNodeId;

    /**
     * 上游节点编码
     */
    private String upstreamNodeCode;

    /**
     * 下游节点ID
     */
    private String downstreamNodeId;

    /**
     * 下游节点编码
     */
    private String downstreamNodeCode;

    /**
     * 依赖类型
     */
    private JobDependencyType dependencyType;

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
     * 校验依赖边定义
     */
    public void validateDefinition() {
        Assert.notBlank(this.workflowId, new SilentException("工作流ID不能为空"));
        boolean hasNodeId = this.upstreamNodeId != null && !this.upstreamNodeId.isBlank()
                && this.downstreamNodeId != null && !this.downstreamNodeId.isBlank();
        boolean hasNodeCode = this.upstreamNodeCode != null && !this.upstreamNodeCode.isBlank()
                && this.downstreamNodeCode != null && !this.downstreamNodeCode.isBlank();
        Assert.isTrue(hasNodeId || hasNodeCode, new SilentException("依赖边上下游节点不能为空"));
        if (hasNodeId) {
            Assert.isTrue(!this.upstreamNodeId.equals(this.downstreamNodeId), new SilentException("节点不能依赖自身"));
        }
        if (hasNodeCode) {
            Assert.isTrue(!this.upstreamNodeCode.equals(this.downstreamNodeCode), new SilentException("节点不能依赖自身"));
        }
        if (this.dependencyType == null) {
            this.dependencyType = JobDependencyType.SCHEDULE;
        }
    }
}
