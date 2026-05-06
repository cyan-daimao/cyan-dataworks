package com.cyan.dataworks.domain.job;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业（Job）领域对象
 *
 * <p>对标 Spark/Flink 中的 Job 概念：一个 Job 代表一个数据加工任务定义，
 * 包含 SQL 内容、引擎类型、调度配置等元数据。每次手动触发或调度触发会产生一个 JobInstance。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Job {

    /**
     * 主键
     */
    private String id;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 作业名称
     */
    private String name;

    /**
     * 作业描述
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
     * 作业状态
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
     * 保存作业
     */
    public Job save(JobRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.notBlank(this.name, new SilentException("作业名称不能为空"));
        Assert.notNull(this.engineType, new SilentException("引擎类型不能为空"));
        Assert.notBlank(this.sqlContent, new SilentException("SQL内容不能为空"));
        if (this.status == null) {
            this.status = TaskStatus.DRAFT;
        }
        return repository.save(this);
    }

    /**
     * 更新作业
     */
    public Job update(JobRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        Assert.notBlank(this.name, new SilentException("作业名称不能为空"));
        Assert.notNull(this.engineType, new SilentException("引擎类型不能为空"));
        Assert.notBlank(this.sqlContent, new SilentException("SQL内容不能为空"));
        return repository.updateById(this);
    }

    /**
     * 删除作业
     */
    public void delete(JobRepository repository) {
        Assert.notBlank(this.id, new SilentException("删除时id不能为空"));
        repository.deleteById(this.id);
    }
}
