package com.cyan.dataworks.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 工作流类型
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
public enum WorkflowType {

    /**
     * 单节点工作流
     */
    SINGLE_NODE("SINGLE_NODE"),

    /**
     * 多节点工作流
     */
    WORKFLOW("WORKFLOW");

    /**
     * 存储值
     */
    @EnumValue
    private final String value;

    WorkflowType(String value) {
        this.value = value;
    }
}
