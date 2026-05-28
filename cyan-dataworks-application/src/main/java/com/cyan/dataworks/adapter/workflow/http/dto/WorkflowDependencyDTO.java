package com.cyan.dataworks.adapter.workflow.http.dto;

import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工作流级依赖DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDependencyDTO {

    /** 工作流ID */
    private String workflowId;

    /** 上游工作流列表 */
    private List<WorkflowDTO> upstreamWorkflows;

    /** 下游工作流列表 */
    private List<WorkflowDTO> downstreamWorkflows;

    /** 依赖类型 */
    private JobDependencyType dependencyType;
}
