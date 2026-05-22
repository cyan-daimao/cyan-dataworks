package com.cyan.dataworks.infra.remote.flink.operator.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Flink Application 提交结果
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlinkApplicationBO {

    /**
     * Deployment 名称
     */
    private String deploymentName;

    /**
     * ConfigMap 名称
     */
    private String configMapName;

    /**
     * K8s 命名空间
     */
    private String namespace;

    /**
     * 状态
     */
    private String status;

    /**
     * 消息
     */
    private String message;
}
