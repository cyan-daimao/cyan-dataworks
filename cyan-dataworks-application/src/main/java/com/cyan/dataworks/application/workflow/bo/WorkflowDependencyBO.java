package com.cyan.dataworks.application.workflow.bo;

import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工作流级依赖业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDependencyBO {

    /** 工作流ID */
    private String workflowId;

    /** 上游工作流列表 */
    private List<WorkflowBO> upstreamWorkflows;

    /** 下游工作流列表 */
    private List<WorkflowBO> downstreamWorkflows;

    /** 依赖类型 */
    private JobDependencyType dependencyType;
}
