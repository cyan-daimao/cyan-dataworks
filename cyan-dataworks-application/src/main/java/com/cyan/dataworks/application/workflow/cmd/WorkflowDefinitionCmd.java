package com.cyan.dataworks.application.workflow.cmd;

import com.cyan.dataworks.enums.JobDependencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工作流定义命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDefinitionCmd {

    /** 节点列表 */
    private List<NodeCmd> nodes;

    /** 依赖边列表 */
    private List<EdgeCmd> edges;

    /**
     * 节点命令对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class NodeCmd {

        /** 主键 */
        private String id;

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
     * 依赖边命令对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class EdgeCmd {

        /** 主键 */
        private String id;

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
