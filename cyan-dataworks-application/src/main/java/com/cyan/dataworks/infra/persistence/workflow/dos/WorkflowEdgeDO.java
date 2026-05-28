package com.cyan.dataworks.infra.persistence.workflow.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流依赖边数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_workflow_edge")
public class WorkflowEdgeDO {

    /** 主键 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 工作流ID */
    @TableField("workflow_id")
    private Long workflowId;

    /** 上游节点ID */
    @TableField("upstream_node_id")
    private Long upstreamNodeId;

    /** 上游节点编码 */
    @TableField("upstream_node_code")
    private String upstreamNodeCode;

    /** 下游节点ID */
    @TableField("downstream_node_id")
    private Long downstreamNodeId;

    /** 下游节点编码 */
    @TableField("downstream_node_code")
    private String downstreamNodeCode;

    /** 依赖类型 */
    @TableField("dependency_type")
    private JobDependencyType dependencyType;

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
