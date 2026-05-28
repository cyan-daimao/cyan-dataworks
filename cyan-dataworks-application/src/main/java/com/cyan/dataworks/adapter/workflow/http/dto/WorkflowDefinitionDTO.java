package com.cyan.dataworks.adapter.workflow.http.dto;

import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工作流定义DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDefinitionDTO {

    /** 工作流ID */
    private String workflowId;

    /** 节点列表 */
    private List<NodeDTO> nodes;

    /** 依赖边列表 */
    private List<EdgeDTO> edges;

    /**
     * 节点DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class NodeDTO {

        /** 主键 */
        private String id;

        /** 工作流ID */
        private String workflowId;

        /** 作业ID */
        private String jobId;

        /** 节点编码 */
        private String nodeCode;

        /** 节点名称 */
        private String nodeName;

        /** X坐标 */
        private Integer positionX;

        /** Y坐标 */
        private Integer positionY;

        /** 节点配置JSON */
        private String configJson;
    }

    /**
     * 依赖边DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class EdgeDTO {

        /** 主键 */
        private String id;

        /** 工作流ID */
        private String workflowId;

        /** 上游节点ID */
        private String upstreamNodeId;

        /** 上游节点编码 */
        private String upstreamNodeCode;

        /** 下游节点ID */
        private String downstreamNodeId;

        /** 下游节点编码 */
        private String downstreamNodeCode;

        /** 依赖类型 */
        private JobDependencyType dependencyType;
    }
}
