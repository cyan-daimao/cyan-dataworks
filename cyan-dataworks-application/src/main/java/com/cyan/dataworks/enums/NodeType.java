package com.cyan.dataworks.enums;

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
     * ODS CDC 流水清洗为 DWD 当前态
     */
    ODS_TO_DWD("ODS_TO_DWD", "ODS到DWD清洗"),

    /**
     * SparkSQL 节点
     */
    SPARK_SQL("SPARK_SQL", "SparkSQL"),

    /**
     * FlinkSQL 节点
     */
    FLINK_SQL("FLINK_SQL", "FlinkSQL"),

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
        return this == ODS_TO_DWD || this == SPARK_SQL || this == FLINK_SQL;
    }

    /**
     * 是否是平台托管输出节点
     */
    public boolean isManagedOutputNode() {
        return this == ODS_TO_DWD || this == SPARK_SQL || this == FLINK_SQL;
    }
}
