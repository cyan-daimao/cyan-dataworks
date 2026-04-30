package com.cyan.dataworks.infra.persistence.execution.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 执行记录数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("execution_record")
public class ExecutionRecordDO {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 任务ID
     */
    @TableField(value = "task_id")
    private Long taskId;

    /**
     * 任务名称
     */
    @TableField(value = "task_name")
    private String taskName;

    /**
     * 引擎类型
     */
    @TableField(value = "engine_type")
    private EngineType engineType;

    /**
     * SQL内容
     */
    @TableField(value = "sql_content")
    private String sqlContent;

    /**
     * 执行状态
     */
    @TableField(value = "status")
    private ExecutionStatus status;

    /**
     * 耗时（毫秒）
     */
    @TableField(value = "cost_time_ms")
    private Long costTimeMs;

    /**
     * 结果数据（JSON）
     */
    @TableField(value = "result_data")
    private String resultData;

    /**
     * 错误信息
     */
    @TableField(value = "error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;
}
