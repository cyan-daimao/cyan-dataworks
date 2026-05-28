package com.cyan.dataworks.infra.persistence.job.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_job")
public class JobDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件夹ID
     */
    @TableField(value = "folder_id")
    private Long folderId;

    /**
     * 作业名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 作业描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 引擎类型
     */
    @TableField(value = "engine_type")
    private EngineType engineType;

    /**
     * 节点类型
     */
    @TableField(value = "node_type")
    private NodeType nodeType;

    /**
     * 任务内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 节点配置JSON
     */
    @TableField(value = "config_json")
    private String configJson;

    /**
     * 作业状态
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
    @TableLogic(value = "null", delval = "now(6)")
    private LocalDateTime deletedAt;
}
