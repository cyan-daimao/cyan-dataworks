package com.cyan.dataworks.infra.persistence.job_instance.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业实例数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_job_instance")
public class JobInstanceDO {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 作业ID
     */
    @TableField(value = "job_id")
    private Long jobId;

    /**
     * 作业名称
     */
    @TableField(value = "job_name")
    private String jobName;

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
     * 创建人
     */
    @TableField(value = "created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    @TableField(value = "updated_by")
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
