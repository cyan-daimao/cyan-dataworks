package com.cyan.dataworks.domain.workflow.query;

import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.dataworks.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 工作流分页查询对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowPageQuery {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流类型
     */
    private WorkflowType workflowType;

    /**
     * 工作流状态
     */
    private TaskStatus status;

    /**
     * 创建人
     */
    private String createdBy;
}
