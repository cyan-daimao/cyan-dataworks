package com.cyan.dataworks.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 工作流触发类型
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
public enum WorkflowTriggerType {

    /**
     * 手动触发
     */
    MANUAL("MANUAL"),

    /**
     * 调度触发
     */
    SCHEDULED("SCHEDULED");

    /**
     * 存储值
     */
    @EnumValue
    private final String value;

    WorkflowTriggerType(String value) {
        this.value = value;
    }
}
