package com.cyan.dataworks.application.job.runtime;

import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.infra.config.FlinkProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 作业执行计划生成器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class JobExecutionPlanner {

    /**
     * Flink配置
     */
    private final FlinkProperties flinkProperties;

    public JobExecutionPlanner(FlinkProperties flinkProperties) {
        this.flinkProperties = flinkProperties;
    }

    /**
     * 生成可提交到执行引擎的SQL
     */
    public String buildExecutableSql(Job job) {
        job.validateDefinition();
        String sql = job.getContent();
        if (job.getEngineType() == EngineType.FLINK) {
            return prependIcebergCatalogIfNeeded(sql);
        }
        return sql;
    }

    /**
     * 按需注入Iceberg Catalog DDL
     */
    private String prependIcebergCatalogIfNeeded(String sql) {
        FlinkProperties.IcebergCatalog catalog = flinkProperties.getIcebergCatalog();
        if (catalog == null || !Boolean.TRUE.equals(catalog.getEnabled())) {
            return sql;
        }
        String catalogName = catalog.getName();
        if (catalogName == null || catalogName.isBlank()) {
            catalogName = "iceberg";
        }
        String normalized = sql.toLowerCase(Locale.ROOT);
        String createCatalogPrefix = "create catalog if not exists " + catalogName.toLowerCase(Locale.ROOT);
        String useCatalogStatement = "use catalog " + catalogName.toLowerCase(Locale.ROOT);
        if (normalized.contains(createCatalogPrefix) && normalized.contains(useCatalogStatement)) {
            return sql;
        }
        return buildIcebergCatalogSql(catalog, catalogName) + "\n\n" + sql;
    }

    /**
     * 构造Iceberg Catalog DDL
     */
    private String buildIcebergCatalogSql(FlinkProperties.IcebergCatalog catalog, String catalogName) {
        return """
                CREATE CATALOG IF NOT EXISTS %s WITH (
                  'type' = '%s',
                  'catalog-type' = '%s',
                  'uri' = '%s',
                  's3.endpoint' = '%s',
                  's3.access-key-id' = '%s',
                  's3.secret-access-key' = '%s',
                  's3.path-style-access' = '%s'
                );

                USE CATALOG %s;
                """.formatted(
                catalogName,
                valueOrDefault(catalog.getType(), "iceberg"),
                valueOrDefault(catalog.getCatalogType(), "rest"),
                valueOrDefault(catalog.getUri(), "http://gravitino-iceberg-rest-server.gravitino.svc.cluster.local:9001/iceberg"),
                valueOrDefault(catalog.getS3Endpoint(), "http://10.0.0.2:9000"),
                valueOrDefault(catalog.getS3AccessKeyId(), "rustfsadmin"),
                valueOrDefault(catalog.getS3SecretAccessKey(), "rustfsadmin"),
                String.valueOf(Boolean.TRUE.equals(catalog.getS3PathStyleAccess())),
                catalogName
        );
    }

    /**
     * 获取配置值或默认值
     */
    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
