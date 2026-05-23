package com.cyan.dataworks.adapter.job_instance.http.dto;

import com.cyan.dataworks.enums.JobLogRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 作业实例日志DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceLogDTO {

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * FlinkDeployment名称
     */
    private String deploymentName;

    /**
     * K8s命名空间
     */
    private String namespace;

    /**
     * 日志角色
     */
    private JobLogRole role;

    /**
     * 日志尾部行数
     */
    private Integer tailLines;

    /**
     * Pod日志列表
     */
    private List<PodLogDTO> pods;

    /**
     * 聚合日志内容
     */
    private String logs;

    /**
     * 提示信息
     */
    private String message;

    /**
     * Pod日志DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class PodLogDTO {

        /**
         * Pod名称
         */
        private String podName;

        /**
         * Pod角色
         */
        private JobLogRole role;

        /**
         * 容器名称
         */
        private String containerName;

        /**
         * 日志内容
         */
        private String log;
    }
}
