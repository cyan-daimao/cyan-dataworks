package com.cyan.dataworks.domain.workflow.query;

import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 工作流实例分页查询对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowInstancePageQuery {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;
}
