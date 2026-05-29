package com.cyan.dataworks.infra.persistence.workflow.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("data_work_workflow")
public class WorkflowDO {

    /** 主键 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 工作流名称 */
    @TableField("name")
    private String name;

    /** 工作流描述 */
    @TableField("description")
    private String description;

    /** Airflow DAG ID */
    @TableField("dag_id")
    private String dagId;

    /** 工作流状态 */
    @TableField("status")
    private TaskStatus status;

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
    @TableLogic(value = "null", delval = "now(6)")
    private LocalDateTime deletedAt;
}
