package com.cyan.dataworks.client.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行状态枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    /**
     * 运行中
     */
    RUNNING("RUNNING", "运行中"),

    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    /**
     * 编码
     */
    private final String code;

    /**
     * 描述
     */
    private final String desc;
}
