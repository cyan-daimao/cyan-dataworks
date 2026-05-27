package com.cyan.dataworks.application.job.lineage;

import com.alibaba.fastjson2.JSON;
import com.cyan.dataman.client.table.dto.MetadataColumnDTO;
import com.cyan.dataworks.infra.gateway.MetadataLineageGateway;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 字段血缘提取器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class SqlFieldLineageExtractor {

    private static final Pattern INSERT_SELECT = Pattern.compile("(?is)\\binsert\\s+into\\s+([`\\w.]+)\\s*(?:\\(([^)]*)\\))?\\s+select\\s+(.+?)\\s+from\\s+([`\\w.]+)");
    private static final Pattern CTAS = Pattern.compile("(?is)\\bcreate\\s+table\\s+([`\\w.]+)\\s+as\\s+select\\s+(.+?)\\s+from\\s+([`\\w.]+)");
    private static final Pattern SELECT_ONLY = Pattern.compile("(?is)^\\s*select\\s+(.+?)\\s+from\\s+([`\\w.]+)");

    private final MetadataLineageGateway metadataLineageGateway;

    public SqlFieldLineageExtractor(MetadataLineageGateway metadataLineageGateway) {
        this.metadataLineageGateway = metadataLineageGateway;
    }

    /**
     * 提取字段血缘
     */
    public ExtractResult extract(String content) {
        try {
            String sql = normalizeSql(content);
            Matcher insert = INSERT_SELECT.matcher(sql);
            if (insert.find()) {
                TableRef target = parseTableRef(insert.group(1));
                List<String> targetColumns = parseColumnList(insert.group(2));
                return buildResult(parseTableRef(insert.group(4)), target, insert.group(3), targetColumns);
            }
            Matcher ctas = CTAS.matcher(sql);
            if (ctas.find()) {
                return buildResult(parseTableRef(ctas.group(3)), parseTableRef(ctas.group(1)), ctas.group(2), List.of());
            }
            Matcher select = SELECT_ONLY.matcher(sql);
            if (select.find()) {
                return buildResult(parseTableRef(select.group(2)), null, select.group(1), List.of());
            }
            return new ExtractResult(List.of(), List.of(), "暂不支持的 SQL 血缘解析格式");
        } catch (Exception e) {
            return new ExtractResult(List.of(), List.of(), e.getMessage());
        }
    }

    /**
     * 构建提取结果
     */
    private ExtractResult buildResult(TableRef sourceTable, TableRef targetTable, String selectPart, List<String> targetColumns) {
        List<SelectItem> selectItems = splitSelectItems(selectPart).stream().map(this::parseSelectItem).toList();
        Set<FieldRef> inputs = new LinkedHashSet<>();
        List<FieldRef> outputs = new ArrayList<>();

        for (int i = 0; i < selectItems.size(); i++) {
            SelectItem item = selectItems.get(i);
            if (item.star()) {
                List<FieldRef> expanded = expandTableFields(sourceTable);
                inputs.addAll(expanded);
                if (targetTable != null) {
                    outputs.addAll(expanded.stream()
                            .map(field -> new FieldRef(targetTable.catalog(), targetTable.schema(), targetTable.table(), field.column()))
                            .toList());
                }
                continue;
            }
            for (String column : item.sourceColumns()) {
                inputs.add(new FieldRef(sourceTable.catalog(), sourceTable.schema(), sourceTable.table(), column));
            }
            if (targetTable != null) {
                String outputColumn = i < targetColumns.size() ? targetColumns.get(i) : item.outputColumn();
                if (outputColumn != null && !outputColumn.isBlank()) {
                    outputs.add(new FieldRef(targetTable.catalog(), targetTable.schema(), targetTable.table(), outputColumn));
                }
            }
        }
        return new ExtractResult(new ArrayList<>(inputs), outputs, null);
    }

    /**
     * 展开表字段
     */
    private List<FieldRef> expandTableFields(TableRef tableRef) {
        List<MetadataColumnDTO> columns = metadataLineageGateway.listColumns(tableRef.catalog(), tableRef.schema(), tableRef.table());
        return columns.stream()
                .map(column -> new FieldRef(tableRef.catalog(), tableRef.schema(), tableRef.table(), column.getCol()))
                .toList();
    }

    /**
     * 解析 SELECT 项
     */
    private SelectItem parseSelectItem(String item) {
        String cleaned = item.trim();
        if ("*".equals(cleaned) || cleaned.endsWith(".*")) {
            return new SelectItem(true, List.of(), null);
        }
        String outputColumn = parseOutputColumn(cleaned);
        List<String> sourceColumns = parseSourceColumns(cleaned);
        return new SelectItem(false, sourceColumns, outputColumn);
    }

    /**
     * 解析输出字段
     */
    private String parseOutputColumn(String item) {
        Matcher asMatcher = Pattern.compile("(?is)\\s+as\\s+`?([A-Za-z_][A-Za-z0-9_]*)`?\\s*$").matcher(item);
        if (asMatcher.find()) {
            return asMatcher.group(1);
        }
        Matcher simpleMatcher = Pattern.compile("(?is)(?:`?[A-Za-z_][A-Za-z0-9_]*`?\\.)?`?([A-Za-z_][A-Za-z0-9_]*)`?\\s*$").matcher(item);
        return simpleMatcher.find() ? simpleMatcher.group(1) : null;
    }

    /**
     * 解析来源字段
     */
    private List<String> parseSourceColumns(String item) {
        String withoutAlias = item.replaceAll("(?is)\\s+as\\s+`?[A-Za-z_][A-Za-z0-9_]*`?\\s*$", "");
        Set<String> columns = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?<!['\"])(?:`?[A-Za-z_][A-Za-z0-9_]*`?\\.)?`?([A-Za-z_][A-Za-z0-9_]*)`?").matcher(withoutAlias);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!isSqlKeyword(token)) {
                columns.add(token);
            }
        }
        return new ArrayList<>(columns);
    }

    /**
     * 判断 SQL 关键字
     */
    private boolean isSqlKeyword(String token) {
        String value = token.toUpperCase(Locale.ROOT);
        return Set.of("SUM", "COUNT", "AVG", "MAX", "MIN", "DISTINCT", "CASE", "WHEN", "THEN", "ELSE", "END", "CAST", "AS", "DATE", "CURRENT_DATE").contains(value);
    }

    /**
     * 解析字段列表
     */
    private List<String> parseColumnList(String columnList) {
        if (columnList == null || columnList.isBlank()) {
            return List.of();
        }
        return splitSelectItems(columnList).stream().map(this::cleanIdentifier).filter(item -> !item.isBlank()).toList();
    }

    /**
     * 拆分 SELECT 项
     */
    private List<String> splitSelectItems(String selectPart) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (char ch : selectPart.toCharArray()) {
            if (ch == '(') {
                depth++;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
            }
            if (ch == ',' && depth == 0) {
                items.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            items.add(current.toString());
        }
        return items;
    }

    /**
     * 解析表引用
     */
    private TableRef parseTableRef(String rawTable) {
        String[] parts = cleanIdentifier(rawTable).split("\\.");
        if (parts.length == 3) {
            return new TableRef(parts[0], parts[1], parts[2]);
        }
        if (parts.length == 2) {
            return new TableRef("iceberg", parts[0], parts[1]);
        }
        return new TableRef("iceberg", "default", parts[0]);
    }

    /**
     * 清理 SQL
     */
    private String normalizeSql(String content) {
        return content == null ? "" : content
                .replaceAll("(?m)--.*$", " ")
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 清理标识符
     */
    private String cleanIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim().replace("`", "");
    }

    /**
     * 字段节点唯一键
     */
    public static String fieldKey(FieldRef field) {
        return "field:" + field.catalog() + "." + field.schema() + "." + field.table() + "." + field.column();
    }

    /**
     * 字段唯一表引用
     */
    public static String tableRef(FieldRef field) {
        return field.catalog() + "." + field.schema() + "." + field.table();
    }

    /**
     * 血缘提取结果
     */
    public record ExtractResult(
            /**
             * 输入字段列表
             */
            List<FieldRef> inputFields,
            /**
             * 输出字段列表
             */
            List<FieldRef> outputFields,
            /**
             * 解析错误信息
             */
            String errorMessage) {

        /**
         * 转为 JSON 属性
         */
        public String errorProperties() {
            return errorMessage == null ? null : JSON.toJSONString(java.util.Map.of("parseError", errorMessage));
        }
    }

    /**
     * 表引用
     */
    private record TableRef(
            /**
             * 元数据 catalog
             */
            String catalog,
            /**
             * 元数据 schema
             */
            String schema,
            /**
             * 表名
             */
            String table) {
    }

    /**
     * 字段引用
     */
    public record FieldRef(
            /**
             * 元数据 catalog
             */
            String catalog,
            /**
             * 元数据 schema
             */
            String schema,
            /**
             * 表名
             */
            String table,
            /**
             * 字段名
             */
            String column) {
    }

    /**
     * SELECT 字段项
     */
    private record SelectItem(
            /**
             * 是否为星号字段
             */
            boolean star,
            /**
             * 来源字段列表
             */
            List<String> sourceColumns,
            /**
             * 输出字段名
             */
            String outputColumn) {
    }
}
