package com.cyan.dataworks.application.job_instance.bo;

import com.cyan.dataworks.enums.JobLogRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 作业实例日志业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceLogBO {

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 日志来源名称
     */
    private String deploymentName;

    /**
     * 日志来源命名空间
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
    private List<PodLogBO> pods;

    /**
     * 聚合日志内容
     */
    private String logs;

    /**
     * 提示信息
     */
    private String message;

    /**
     * Pod日志业务对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class PodLogBO {

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
