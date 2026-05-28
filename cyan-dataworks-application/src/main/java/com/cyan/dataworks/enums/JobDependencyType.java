package com.cyan.dataworks.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 作业依赖类型
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
public enum JobDependencyType {

    /**
     * 调度依赖
     */
    SCHEDULE("SCHEDULE"),

    /**
     * 同周期工作流依赖
     */
    SCHEDULE_SAME_CYCLE("SCHEDULE_SAME_CYCLE");

    /**
     * 存储值
     */
    @EnumValue
    private final String value;

    JobDependencyType(String value) {
        this.value = value;
    }
}
