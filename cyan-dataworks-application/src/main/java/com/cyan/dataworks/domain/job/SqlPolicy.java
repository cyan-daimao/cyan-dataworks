package com.cyan.dataworks.domain.job;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SQL 安全策略
 *
 * @author cy.Y
 * @since 1.0.0
 */
public final class SqlPolicy {

    /**
     * 平台禁止用户直接执行的 DDL/DML 关键字
     */
    private static final Pattern FORBIDDEN_KEYWORD_PATTERN = Pattern.compile(
            "(^|\\s|;)(CREATE|ALTER|DROP|TRUNCATE|INSERT|UPDATE|DELETE|MERGE|REPLACE|CALL|GRANT|REVOKE)\\s+",
            Pattern.CASE_INSENSITIVE
    );

    private SqlPolicy() {
    }

    /**
     * 校验用户 SQL 只能是查询语义
     */
    public static void assertSelectOnly(String sql) {
        Assert.notBlank(sql, new SilentException("SQL内容不能为空"));
        String normalized = sql.trim();
        Assert.isFalse(FORBIDDEN_KEYWORD_PATTERN.matcher(normalized).find(), new SilentException("禁止在数据开发中直接执行DDL/DML，请通过元数据平台管理表结构，由平台生成写入语句"));
        String upperSql = normalized.toUpperCase(Locale.ROOT);
        Assert.isTrue(upperSql.startsWith("SELECT") || upperSql.startsWith("WITH"), new SilentException("当前节点只允许编写SELECT查询"));
    }

    /**
     * 判断 SQL 是否为空
     */
    public static boolean isBlank(String sql) {
        return StrUtils.isBlank(sql);
    }
}
