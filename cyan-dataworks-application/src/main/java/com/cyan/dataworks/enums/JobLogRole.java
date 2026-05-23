package com.cyan.dataworks.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作业日志角色枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum JobLogRole {

    /**
     * 全部角色
     */
    ALL("ALL", "全部"),

    /**
     * JobManager
     */
    JOB_MANAGER("JOB_MANAGER", "JobManager"),

    /**
     * TaskManager
     */
    TASK_MANAGER("TASK_MANAGER", "TaskManager");

    /**
     * 编码
     */
    private final String code;

    /**
     * 描述
     */
    private final String desc;
}
