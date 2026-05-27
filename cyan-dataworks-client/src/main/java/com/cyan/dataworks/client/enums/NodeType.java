package com.cyan.dataworks.client.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据加工节点类型枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum NodeType {

    /**
     * SparkSQL 节点
     */
    SPARK_SQL("SPARK_SQL", "SparkSQL"),

    /**
     * FlinkSQL 节点
     */
    FLINK_SQL("FLINK_SQL", "FlinkSQL"),

    /**
     * Spark批任务节点
     */
    SPARK_BATCH("SPARK_BATCH", "Spark批任务"),

    /**
     * Flink批任务节点
     */
    FLINK_BATCH("FLINK_BATCH", "Flink批任务"),

    /**
     * Shell脚本节点
     */
    SHELL("SHELL", "Shell"),

    /**
     * Python 节点
     */
    PYTHON("PYTHON", "Python"),

    /**
     * 数据质量节点
     */
    DATA_QUALITY("DATA_QUALITY", "数据质量"),

    /**
     * 虚拟节点
     */
    VIRTUAL("VIRTUAL", "虚拟节点");

    /**
     * 编码
     */
    private final String code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 是否是 SQL 类节点
     */
    public boolean isSqlNode() {
        return this == SPARK_SQL || this == FLINK_SQL;
    }

    /**
     * 是否是平台托管输出节点
     */
    public boolean isManagedOutputNode() {
        return this == SPARK_SQL || this == FLINK_SQL;
    }
}
