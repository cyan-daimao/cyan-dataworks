package com.cyan.dataworks.application.workflow.bo;

import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.dataworks.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowBO {

    /** 主键 */
    private String id;

    /** 工作流名称 */
    private String name;

    /** 工作流描述 */
    private String description;

    /** 工作流类型 */
    private WorkflowType workflowType;

    /** DAG ID */
    private String dagId;

    /** 工作流状态 */
    private TaskStatus status;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
