package com.cyan.dataworks.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 引擎类型枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum EngineType {

    /**
     * SparkSQL 引擎
     */
    SPARK("SPARK", "SparkSQL"),

    /**
     * FlinkSQL 引擎
     */
    FLINK("FLINK", "FlinkSQL"),

    /**
     * Shell脚本引擎
     */
    SHELL("SHELL", "Shell"),

    /**
     * Python脚本引擎
     */
    PYTHON("PYTHON", "Python");

    /**
     * 编码
     */
    private final String code;

    /**
     * 描述
     */
    private final String desc;
}
