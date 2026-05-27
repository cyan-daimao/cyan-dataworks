package com.cyan.dataworks.client.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度器类型枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SchedulerType {

    /**
     * DataWorks内置调度器
     */
    INTERNAL("INTERNAL", "内置调度器"),

    /**
     * Airflow调度器
     */
    AIRFLOW("AIRFLOW", "Airflow");

    /**
     * 编码
     */
    private final String code;

    /**
     * 描述
     */
    private final String desc;
}
