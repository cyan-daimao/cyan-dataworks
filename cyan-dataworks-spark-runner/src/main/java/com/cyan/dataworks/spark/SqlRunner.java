package com.cyan.dataworks.spark;

import org.apache.spark.sql.SparkSession;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * DataWorks Spark SQL Runner
 *
 * <p>SparkApplication 入口类。接收 SQL 文件路径，逐条执行 SQL。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
public class SqlRunner {

    /**
     * 应用入口
     *
     * @param args 启动参数，第一个参数为SQL文件路径
     * @throws Exception 执行异常
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: SqlRunner <sql-file-path>");
        }

        String sql = Files.readString(Paths.get(args[0]));
        List<String> statements = splitStatements(removeLineComments(sql));
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("SQL文件中没有可执行语句");
        }

        SparkSession spark = SparkSession.builder()
                .appName("dataworks-spark-sql")
                .getOrCreate();
        try {
            for (String statement : statements) {
                System.out.println("Executing Spark SQL: " + statement);
                spark.sql(statement);
            }
        } finally {
            spark.stop();
        }
    }

    /**
     * 移除SQL单行注释
     *
     * @param sql 原始SQL
     * @return 清理后的SQL
     */
    private static String removeLineComments(String sql) {
        return sql.replaceAll("(?m)^\\s*--.*\\n?", "");
    }

    /**
     * 按分号拆分SQL语句
     *
     * @param sql SQL文本
     * @return SQL语句列表
     */
    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        for (String segment : sql.split(";")) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }
}
