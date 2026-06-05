package com.cyan.dataworks.infra.remote.spark.operator.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * SparkApplication业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SparkApplicationBO {

    /**
     * SparkApplication名称
     */
    private String applicationName;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * SQL ConfigMap名称
     */
    private String configMapName;

    /**
     * Driver Pod名称
     */
    private String driverPodName;

    /**
     * 当前状态
     */
    private String state;

    /**
     * 是否运行中
     */
    private Boolean running;

    /**
     * 是否成功完成
     */
    private Boolean completed;

    /**
     * 是否失败
     */
    private Boolean failed;

    /**
     * 状态说明
     */
    private String message;
}
