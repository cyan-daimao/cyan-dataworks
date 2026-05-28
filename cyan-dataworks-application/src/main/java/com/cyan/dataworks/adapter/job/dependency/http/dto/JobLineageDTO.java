package com.cyan.dataworks.adapter.job.dependency.http.dto;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.JobDependencyType;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 作业血缘 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobLineageDTO {

    /**
     * 当前作业ID
     */
    private String jobId;

    /**
     * 血缘节点列表
     */
    private List<NodeDTO> nodes;

    /**
     * 血缘边列表
     */
    private List<EdgeDTO> edges;

    /**
     * 作业血缘节点
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class NodeDTO {

        /**
         * 作业ID
         */
        private String jobId;

        /**
         * 作业名称
         */
        private String jobName;

        /**
         * 引擎类型
         */
        private EngineType engineType;

        /**
         * 节点类型
         */
        private NodeType nodeType;

        /**
         * 作业状态
         */
        private TaskStatus status;
    }

    /**
     * 作业血缘边
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class EdgeDTO {

        /**
         * 上游作业ID
         */
        private String upstreamJobId;

        /**
         * 下游作业ID
         */
        private String downstreamJobId;

        /**
         * 依赖类型
         */
        private JobDependencyType dependencyType;
    }
}
