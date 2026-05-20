package com.cyan.dataworks.application.job.runtime;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.SqlPolicy;
import com.cyan.dataworks.enums.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 作业执行计划生成器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class JobExecutionPlanner {

    /**
     * JSON解析器
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建作业执行计划生成器
     */
    public JobExecutionPlanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 生成可提交到执行引擎的SQL
     */
    public String buildExecutableSql(Job job) {
        job.validateDefinition();
        if (job.getNodeType() == NodeType.ODS_TO_DWD) {
            return buildOdsToDwdSql(job);
        }
        return job.getSqlContent();
    }

    /**
     * 生成ODS到DWD清洗SQL
     */
    private String buildOdsToDwdSql(Job job) {
        OdsToDwdNodeConfig config = parseOdsToDwdConfig(job.getConfigJson());
        validateOdsToDwdConfig(config);
        SqlPolicy.assertSelectOnly(job.getSqlContent());
        String pkCondition = config.getPrimaryKeys().stream()
                .map(pk -> config.getOutputTable() + "." + pk + " = _deleted." + pk)
                .collect(Collectors.joining(" AND "));
        return """
                -- Cyan DataWorks generated ODS_TO_DWD plan
                -- upsert: _op in ('c','r','u') writes the current row into DWD
                INSERT INTO %s
                SELECT *
                FROM (
                %s
                ) _cyan_src
                WHERE _cyan_src.%s IN ('c', 'r', 'u');

                -- delete: _op = 'd' physically removes the DWD row by primary key
                DELETE FROM %s
                WHERE EXISTS (
                    SELECT 1
                    FROM %s _deleted
                    WHERE _deleted.%s = 'd'
                      AND %s
                );
                """.formatted(
                config.getOutputTable(),
                trimTrailingSemicolon(job.getSqlContent()),
                config.getOpField(),
                config.getOutputTable(),
                config.getInputTable(),
                config.getOpField(),
                pkCondition
        );
    }

    /**
     * 解析ODS到DWD配置
     */
    private OdsToDwdNodeConfig parseOdsToDwdConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, OdsToDwdNodeConfig.class);
        } catch (Exception e) {
            throw new SilentException("ODS到DWD节点配置JSON格式不正确");
        }
    }

    /**
     * 校验ODS到DWD配置
     */
    private void validateOdsToDwdConfig(OdsToDwdNodeConfig config) {
        Assert.notNull(config, new SilentException("ODS到DWD节点配置不能为空"));
        Assert.notBlank(config.getInputTable(), new SilentException("输入ODS表不能为空"));
        Assert.notBlank(config.getOutputTable(), new SilentException("输出DWD表不能为空"));
        Assert.isTrue(config.getPrimaryKeys() != null && !config.getPrimaryKeys().isEmpty(), new SilentException("主键字段不能为空"));
        config.setOpField(defaultIfBlank(config.getOpField(), "_op"));
        config.setEventTimeField(defaultIfBlank(config.getEventTimeField(), "_ts"));
        config.setIngestionTimeField(defaultIfBlank(config.getIngestionTimeField(), "_ingestion_time"));
        List<String> primaryKeys = Optional.ofNullable(config.getPrimaryKeys()).orElse(List.of()).stream()
                .filter(StrUtils::isNotBlank)
                .map(String::trim)
                .toList();
        Assert.isTrue(!primaryKeys.isEmpty(), new SilentException("主键字段不能为空"));
        config.setPrimaryKeys(primaryKeys);
    }

    /**
     * 去掉SQL末尾分号
     */
    private String trimTrailingSemicolon(String sql) {
        String value = sql.trim();
        while (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    /**
     * 空值兜底
     */
    private String defaultIfBlank(String value, String defaultValue) {
        return StrUtils.isBlank(value) ? defaultValue : value.trim();
    }
}
