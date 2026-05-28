package com.cyan.dataworks.application.workflow.bo;

import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.WorkflowTriggerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流实例业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowInstanceBO {

    /** 主键 */
    private String id;

    /** 工作流ID */
    private String workflowId;

    /** 工作流名称 */
    private String workflowName;

    /** DAG ID */
    private String dagId;

    /** DAG运行ID */
    private String dagRunId;

    /** 执行状态 */
    private ExecutionStatus status;

    /** 触发类型 */
    private WorkflowTriggerType triggerType;

    /** 耗时（毫秒） */
    private Long costTimeMs;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
