package com.cyan.dataworks.domain.execution;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.execution.repository.ExecutionRecordRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 执行记录领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ExecutionRecord {

    /**
     * 主键
     */
    private String id;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * SQL内容
     */
    private String sqlContent;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 耗时（毫秒）
     */
    private Long costTimeMs;

    /**
     * 结果数据（JSON）
     */
    private String resultData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 保存执行记录
     */
    public ExecutionRecord save(ExecutionRecordRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.notBlank(this.taskId, new SilentException("任务ID不能为空"));
        Assert.notNull(this.engineType, new SilentException("引擎类型不能为空"));
        Assert.notBlank(this.sqlContent, new SilentException("SQL内容不能为空"));
        Assert.notNull(this.status, new SilentException("执行状态不能为空"));
        return repository.save(this);
    }

    /**
     * 更新执行记录
     */
    public ExecutionRecord update(ExecutionRecordRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        return repository.updateById(this);
    }
}
