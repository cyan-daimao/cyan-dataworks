package com.cyan.dataworks.infra.persistence.task.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工任务数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_task")
public class DataWorkTaskDO {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 任务名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 任务描述
     */
    @TableField(value = "description")
    private String description;

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
     * 任务状态
     */
    @TableField(value = "status")
    private TaskStatus status;

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
