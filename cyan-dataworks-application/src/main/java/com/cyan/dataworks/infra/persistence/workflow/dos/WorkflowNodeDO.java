package com.cyan.dataworks.infra.persistence.workflow.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流节点数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_workflow_node")
public class WorkflowNodeDO {

    /** 主键 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 工作流ID */
    @TableField("workflow_id")
    private Long workflowId;

    /** 作业ID */
    @TableField("job_id")
    private Long jobId;

    /** 节点编码 */
    @TableField("node_code")
    private String nodeCode;

    /** 节点名称 */
    @TableField("node_name")
    private String nodeName;

    /** X坐标 */
    @TableField("position_x")
    private Integer positionX;

    /** Y坐标 */
    @TableField("position_y")
    private Integer positionY;

    /** 节点配置JSON */
    @TableField("config_json")
    private String configJson;

    /** 创建人 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新人 */
    @TableField("updated_by")
    private String updatedBy;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 删除时间 */
    @TableField("deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
