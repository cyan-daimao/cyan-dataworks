package com.cyan.dataworks.domain.job.dependency;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job.dependency.repository.JobDependencyRepository;
import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 作业依赖领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDependency {

    /**
     * 主键
     */
    private String id;

    /**
     * 上游作业ID
     */
    private String upstreamJobId;

    /**
     * 下游作业ID
     */
    private String downstreamJobId;

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
     * 保存作业依赖
     */
    public JobDependency save(JobDependencyRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        validateDefinition();
        return repository.save(this);
    }

    /**
     * 校验依赖定义
     */
    public void validateDefinition() {
        Assert.notBlank(this.upstreamJobId, new SilentException("上游作业ID不能为空"));
        Assert.notBlank(this.downstreamJobId, new SilentException("下游作业ID不能为空"));
        Assert.isTrue(!this.upstreamJobId.equals(this.downstreamJobId), new SilentException("作业不能依赖自身"));
        if (this.dependencyType == null) {
            this.dependencyType = JobDependencyType.SCHEDULE;
        }
    }
}
