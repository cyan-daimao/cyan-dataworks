package com.cyan.dataworks.domain.job;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;

import java.util.Arrays;
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
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 CREATE 操作
     */
    private static final Pattern FORBIDDEN_CREATE_LAYER_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+)?(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 ALTER 操作
     */
    private static final Pattern FORBIDDEN_ALTER_LAYER_PATTERN = Pattern.compile(
            "ALTER\\s+TABLE\\s+(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 DROP 操作
     */
    private static final Pattern FORBIDDEN_DROP_LAYER_PATTERN = Pattern.compile(
            "DROP\\s+TABLE\\s+(IF\\s+EXISTS\\s+)?(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 TRUNCATE 操作
     */
    private static final Pattern FORBIDDEN_TRUNCATE_LAYER_PATTERN = Pattern.compile(
            "TRUNCATE\\s+TABLE\\s+(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 INSERT 操作
     */
    private static final Pattern FORBIDDEN_INSERT_LAYER_PATTERN = Pattern.compile(
            "INSERT\\s+(INTO|OVERWRITE)\\s+(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 UPDATE 操作
     */
    private static final Pattern FORBIDDEN_UPDATE_LAYER_PATTERN = Pattern.compile(
            "UPDATE\\s+(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行的 DELETE 操作
     */
    private static final Pattern FORBIDDEN_DELETE_LAYER_PATTERN = Pattern.compile(
            "DELETE\\s+FROM\\s+(ods_|dwd_|dws_|ads_)\\w+",
            Pattern.CASE_INSENSITIVE
    );

    private SqlPolicy() {
    }

    /**
     * 校验用户 SQL：
     * 禁止对数仓分层表(ods/dwd/dws/ads)执行 DDL/DML，非分层表的 Flink SQL 允许执行
     */
    public static void assertSelectOnly(String sql) {
        Assert.notBlank(sql, new SilentException("任务内容不能为空"));
        String normalized = sql.trim();

        // 1. 禁止对数仓分层表执行 DDL/DML
        if (FORBIDDEN_CREATE_LAYER_PATTERN.matcher(normalized).find()
                || FORBIDDEN_ALTER_LAYER_PATTERN.matcher(normalized).find()
                || FORBIDDEN_DROP_LAYER_PATTERN.matcher(normalized).find()
                || FORBIDDEN_TRUNCATE_LAYER_PATTERN.matcher(normalized).find()
                || FORBIDDEN_INSERT_LAYER_PATTERN.matcher(normalized).find()
                || FORBIDDEN_UPDATE_LAYER_PATTERN.matcher(normalized).find()
                || FORBIDDEN_DELETE_LAYER_PATTERN.matcher(normalized).find()) {
            throw new SilentException("禁止在数据开发中直接对数仓分层表(ods/dwd/dws/ads)执行DDL/DML，请通过元数据平台管理表结构，由平台生成写入语句");
        }

        // 非分层表的 Flink SQL（CREATE TEMPORARY TABLE、INSERT INTO 非分层表等）允许执行
        // 数仓分层表(ods/dwd/dws/ads)的 DDL/DML 由平台统一管理
    }

    /**
     * 校验Flink Application模式SQL
     */
    public static void assertFlinkApplicationSql(String sql) {
        Assert.notBlank(sql, new SilentException("任务内容不能为空"));
        String normalized = removeComments(sql);
        boolean hasInsert = Arrays.stream(normalized.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isBlank())
                .map(statement -> statement.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT))
                .anyMatch(statement -> statement.startsWith("INSERT "));
        if (!hasInsert) {
            throw new SilentException("FlinkSQL正式任务必须包含 INSERT INTO ... SELECT ...，SELECT 查询请使用临时运行/预览");
        }
        boolean hasSelectOnly = Arrays.stream(normalized.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isBlank())
                .map(statement -> statement.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT))
                .anyMatch(statement -> statement.startsWith("SELECT ") || statement.startsWith("WITH "));
        if (hasSelectOnly) {
            throw new SilentException("FlinkSQL正式任务不支持直接执行 SELECT/WITH 查询，请改为 INSERT INTO sink_table SELECT ...");
        }
    }

    /**
     * 判断 SQL 是否为空
     */
    public static boolean isBlank(String sql) {
        return StrUtils.isBlank(sql);
    }

    /**
     * 移除SQL注释
     * <p>
     * 先去除块注释 {@code /* ... *\/}，再去除行注释 {@code -- ...}。
     * 行注释从 {@code --} 一直删到行尾且保留换行，既能处理整行注释，
     * 也能处理写在语句尾部的内联注释，避免按 {@code ;} 切分时把行尾注释和
     * 后续语句粘连成同一段（导致 INSERT 被误判为缺失）。
     */
    private static String removeComments(String sql) {
        String withoutBlockComments = sql.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)--[^\\n]*", "");
    }
}
