package com.cyan.dataworks.infra.remote.flink;

import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.JSON;
import com.cyan.dataworks.infra.config.FlinkProperties;
import com.cyan.dataworks.enums.JobLogRole;
import com.cyan.dataworks.infra.remote.flink.client.FlinkRpcClient;
import com.cyan.dataworks.infra.remote.flink.operator.FlinkApplicationOperatorService;
import com.cyan.dataworks.infra.remote.flink.operator.bo.FlinkApplicationBO;
import com.cyan.dataworks.infra.remote.flink.operator.bo.FlinkPodLogBO;
import com.cyan.dataworks.infra.remote.flink.operator.cmd.FlinkApplicationSubmitCmd;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Flink SQL Gateway REST API 远程服务
 *
 * <p>封装 Flink SQL Gateway 的会话管理、语句执行、结果轮询与资源清理等业务逻辑，
 * 底层 HTTP 调用通过 {@link FlinkRpcClient}（OpenFeign）完成。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class FlinkRemoteService {

    /**
     * JSON解析器
     */
    private final ObjectMapper objectMapper;

    /**
     * Flink配置属性
     */
    private final FlinkProperties flinkProperties;

    /**
     * Flink Gateway Feign 客户端
     */
    private final FlinkRpcClient flinkRpcClient;

    /**
     * Flink Kubernetes Operator 服务
     */
    private final FlinkApplicationOperatorService flinkApplicationOperatorService;

    /**
     * 创建Flink SQL Gateway远程服务
     *
     * @param objectMapper                  JSON解析器
     * @param flinkProperties               Flink配置属性
     * @param flinkRpcClient                Flink Gateway Feign 客户端
     * @param flinkApplicationOperatorService Flink Kubernetes Operator 服务
     */
    public FlinkRemoteService(ObjectMapper objectMapper, FlinkProperties flinkProperties,
                              FlinkRpcClient flinkRpcClient,
                              FlinkApplicationOperatorService flinkApplicationOperatorService) {
        this.objectMapper = objectMapper;
        this.flinkProperties = flinkProperties;
        this.flinkRpcClient = flinkRpcClient;
        this.flinkApplicationOperatorService = flinkApplicationOperatorService;
    }

    /**
     * 通过SQL Gateway临时执行FlinkSQL
     *
     * @param sql SQL语句
     * @return 执行结果JSON
     */
    public String executeSql(String sql) {
        String gatewayUrl = getGatewayUrl();
        if (gatewayUrl.isEmpty()) {
            log.warn("Flink REST URL未配置，返回mock结果");
            return mockExecuteSql(sql);
        }

        String sessionHandle = null;
        String operationHandle = null;
        long startTime = System.currentTimeMillis();
        try {
            sessionHandle = openSession();
            List<String> statements = splitStatements(sql);
            AssertStatements.notEmpty(statements);
            List<Map<String, Object>> executedStatements = new ArrayList<>();
            String lastResult = "{}";
            for (int i = 0; i < statements.size(); i++) {
                boolean last = i == statements.size() - 1;
                String statement = statements.get(i);
                operationHandle = executeStatement(sessionHandle, statement);
                lastResult = fetchOperationResult(sessionHandle, operationHandle, statement);
                executedStatements.add(Map.of(
                        "statement", statement,
                        "operationHandle", operationHandle,
                        "result", parseJsonOrRaw(lastResult)
                ));
                if (!last) {
                    closeOperation(sessionHandle, operationHandle);
                    operationHandle = null;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mock", false);
            result.put("mode", "SESSION");
            result.put("sessionHandle", sessionHandle);
            result.put("operationHandle", operationHandle);
            result.put("result", parseJsonOrRaw(lastResult));
            result.put("durationMs", System.currentTimeMillis() - startTime);
            result.put("statements", executedStatements);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("FlinkSQL临时执行失败", e);
            throw new SilentException("FlinkSQL临时执行失败：" + e.getMessage());
        } finally {
            closeQuietly(sessionHandle, operationHandle);
        }
    }

    /**
     * 以Application Mode提交FlinkSQL正式作业
     *
     * @param jobName 作业名称
     * @param sql     SQL语句
     * @return 提交结果JSON
     */
    public String submitApplication(String jobId, String jobName, String sql) {
        String deploymentName = toTrackingDeploymentName(jobId);
        String configMapName = deploymentName + "-sql";

        FlinkApplicationSubmitCmd cmd = new FlinkApplicationSubmitCmd()
                .setJobName(jobName)
                .setDeploymentName(deploymentName)
                .setConfigMapName(configMapName)
                .setSql(sql);

        FlinkApplicationBO result = flinkApplicationOperatorService.submit(cmd);

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("mock", false);
        resultMap.put("mode", "APPLICATION");
        resultMap.put("deploymentName", result.getDeploymentName());
        resultMap.put("configMapName", result.getConfigMapName());
        resultMap.put("namespace", result.getNamespace());
        resultMap.put("status", result.getStatus());
        resultMap.put("message", result.getMessage());
        resultMap.put("jobManagerPodName", result.getJobManagerPodName());
        resultMap.put("taskManagerPodNames", Optional.ofNullable(result.getTaskManagerPodNames()).orElse(List.of()));
        return JSON.toJSONString(resultMap);
    }

    /**
     * 查询Application Mode作业Pod日志
     *
     * @param deploymentName FlinkDeployment名称
     * @param namespace      命名空间
     * @param role           日志角色
     * @param tailLines      尾部行数
     * @param previous       是否读取上一个已终止容器日志
     * @return Pod日志列表
     */
    public List<FlinkPodLogBO> getApplicationPodLogs(String deploymentName,
                                                     String namespace,
                                                     JobLogRole role,
                                                     int tailLines,
                                                     boolean previous) {
        return flinkApplicationOperatorService.getPodLogs(deploymentName, namespace, role, tailLines, previous);
    }

    /**
     * 删除Application Mode作业
     *
     * @param deploymentName FlinkDeployment名称
     * @param configMapName  ConfigMap名称
     */
    public void deleteApplication(String deploymentName, String configMapName) {
        flinkApplicationOperatorService.delete(deploymentName, configMapName);
    }

    /**
     * Mock执行FlinkSQL（当Flink集群未启动时使用）
     *
     * @param sql SQL语句
     * @return mock结果JSON
     */
    private String mockExecuteSql(String sql) {
        Map<String, Object> result = Map.of(
                "mock", true,
                "mode", "SESSION",
                "sql", sql,
                "message", "Flink集群未连接，返回mock结果"
        );
        return JSON.toJSONString(result);
    }

    /**
     * 获取SQL Gateway地址
     *
     * @return SQL Gateway地址
     */
    private String getGatewayUrl() {
        return Optional.ofNullable(flinkProperties)
                .map(FlinkProperties::getRest)
                .map(FlinkProperties.Rest::getUrl)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.endsWith("/") ? value.substring(0, value.length() - 1) : value)
                .orElse("");
    }

    /**
     * 打开SQL Gateway session
     *
     * @return sessionHandle
     */
    private String openSession() throws Exception {
        URI uri = URI.create(getGatewayUrl() + "/v1/sessions");
        String response = flinkRpcClient.post(uri, Map.of());
        OpenSessionResponse body = readResponse(response, OpenSessionResponse.class);
        if (body == null || body.getSessionHandle() == null || body.getSessionHandle().isBlank()) {
            throw new SilentException("SQL Gateway打开session失败");
        }
        return body.getSessionHandle();
    }

    /**
     * 执行单条SQL语句
     *
     * @param sessionHandle session标识
     * @param statement     SQL语句
     * @return operationHandle
     */
    private String executeStatement(String sessionHandle, String statement) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statement", statement);
        URI uri = URI.create(getGatewayUrl() + "/v1/sessions/" + encode(sessionHandle) + "/statements");
        String response = flinkRpcClient.post(uri, body);
        ExecuteStatementResponse result = readResponse(response, ExecuteStatementResponse.class);
        if (result == null || result.getOperationHandle() == null || result.getOperationHandle().isBlank()) {
            throw new SilentException("SQL Gateway提交SQL失败");
        }
        return result.getOperationHandle();
    }

    /**
     * 拉取operation执行结果
     *
     * @param sessionHandle   session标识
     * @param operationHandle operation标识
     * @param statement       SQL语句
     * @return operation结果JSON
     */
    private String fetchOperationResult(String sessionHandle, String operationHandle, String statement) {
        if (!isQueryStatement(statement)) {
            return getOperationStatus(sessionHandle, operationHandle);
        }
        String resultUri = "/v1/sessions/" + encode(sessionHandle)
                + "/operations/" + encode(operationHandle)
                + "/result/0?rowFormat=JSON";
        List<Object> data = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        Object rawColumns = List.of();
        List<Object> pages = new ArrayList<>();
        String resultKind = "";
        String jobId = "";
        int readPages = 0;
        while (readPages < getPreviewMaxResultPages() && data.size() < getPreviewMaxResultRows()) {
            String body = fetchReadyResultPage(resultUri);
            if (body == null || body.isBlank()) {
                break;
            }
            readPages++;
            pages.add(parseJsonOrRaw(body));
            JsonNode page = parseJsonNode(body);
            if (page == null) {
                break;
            }
            jobId = firstText(jobId, page, "jobID");
            resultKind = firstText(resultKind, page, "resultKind");
            JsonNode results = page.get("results");
            if (results != null) {
                if (results.has("columns")) {
                    rawColumns = objectMapper.convertValue(results.get("columns"), Object.class);
                    columnNames = normalizeColumnNames(results.get("columns"));
                }
                JsonNode rows = results.get("data");
                if (rows != null && rows.isArray()) {
                    for (JsonNode row : rows) {
                        if (data.size() >= getPreviewMaxResultRows()) {
                            break;
                        }
                        data.add(normalizeRow(row, columnNames));
                    }
                }
            }
            if ("EOS".equals(page.path("resultType").asText())) {
                break;
            }
            String nextResultUri = page.path("nextResultUri").asText("");
            if (nextResultUri.isBlank()) {
                break;
            }
            resultUri = appendRowFormat(nextResultUri);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resultType", "PAYLOAD");
        result.put("isQueryResult", true);
        result.put("jobID", jobId);
        result.put("resultKind", resultKind);
        result.put("columns", columnNames);
        result.put("rows", data);
        result.put("total", data.size());
        result.put("maxRowsReached", data.size() >= getPreviewMaxResultRows());
        result.put("results", Map.of(
                "columns", rawColumns,
                "rowFormat", "JSON",
                "data", data
        ));
        result.put("pages", pages);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new SilentException("FlinkSQL结果序列化失败：" + e.getMessage());
        }
    }

    /**
     * 拉取已就绪的结果页
     *
     * @param resultUri 结果页相对路径
     * @return 结果页JSON
     */
    private String fetchReadyResultPage(String resultUri) {
        URI uri = URI.create(getGatewayUrl() + resultUri);
        int pollTimes = getPreviewPollTimes();
        for (int i = 0; i < pollTimes; i++) {
            String body = flinkRpcClient.get(uri);
            if (body != null && !body.contains("\"resultType\":\"NOT_READY\"")) {
                return body;
            }
            sleep(getPreviewPollIntervalMs());
        }
        return "";
    }

    /**
     * 查询operation状态
     *
     * @param sessionHandle   session标识
     * @param operationHandle operation标识
     * @return operation状态JSON
     */
    private String getOperationStatus(String sessionHandle, String operationHandle) {
        URI uri = URI.create(getGatewayUrl() + "/v1/sessions/" + encode(sessionHandle)
                + "/operations/" + encode(operationHandle) + "/status");
        return flinkRpcClient.get(uri);
    }

    /**
     * 关闭operation
     *
     * @param sessionHandle   session标识
     * @param operationHandle operation标识
     */
    private void closeOperation(String sessionHandle, String operationHandle) {
        URI uri = URI.create(getGatewayUrl() + "/v1/sessions/" + encode(sessionHandle)
                + "/operations/" + encode(operationHandle) + "/close");
        flinkRpcClient.delete(uri, Map.of());
    }

    /**
     * 关闭session
     *
     * @param sessionHandle session标识
     */
    private void closeSession(String sessionHandle) {
        URI uri = URI.create(getGatewayUrl() + "/v1/sessions/" + encode(sessionHandle));
        flinkRpcClient.delete(uri, Map.of());
    }

    /**
     * 安静关闭SQL Gateway资源
     *
     * @param sessionHandle   session标识
     * @param operationHandle operation标识
     */
    private void closeQuietly(String sessionHandle, String operationHandle) {
        try {
            if (sessionHandle != null && operationHandle != null) {
                closeOperation(sessionHandle, operationHandle);
            }
        } catch (Exception e) {
            log.warn("关闭Flink SQL Gateway operation失败: {}", e.getMessage());
        }
        try {
            if (sessionHandle != null) {
                closeSession(sessionHandle);
            }
        } catch (Exception e) {
            log.warn("关闭Flink SQL Gateway session失败: {}", e.getMessage());
        }
    }

    /**
     * 读取JSON响应
     *
     * @param body  响应体
     * @param clazz 响应类型
     * @return 响应对象
     */
    private <T> T readResponse(String body, Class<T> clazz) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        return objectMapper.readValue(body, clazz);
    }

    /**
     * 将JSON字符串转为对象，失败时保留原始字符串
     *
     * @param value JSON或普通字符串
     * @return JSON节点或原始字符串
     */
    private Object parseJsonOrRaw(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 解析JSON节点
     *
     * @param value JSON字符串
     * @return JSON节点
     */
    private JsonNode parseJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从SQL Gateway列元数据中提取前端展示列名
     *
     * @param columns 列元数据节点
     * @return 列名列表
     */
    private List<String> normalizeColumnNames(JsonNode columns) {
        List<String> names = new ArrayList<>();
        if (columns == null || !columns.isArray()) {
            return names;
        }
        int index = 0;
        for (JsonNode column : columns) {
            String name = column.path("name").asText("");
            if (name.isBlank()) {
                name = column.path("columnName").asText("");
            }
            if (name.isBlank()) {
                name = "col_" + index;
            }
            names.add(name);
            index++;
        }
        return names;
    }

    /**
     * 将SQL Gateway行数据规整为前端可直接渲染的Map
     *
     * @param row     行数据节点
     * @param columns 列名列表
     * @return 行Map
     */
    private Map<String, Object> normalizeRow(JsonNode row, List<String> columns) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        JsonNode fields = row.has("fields") ? row.get("fields") : row;
        if (fields != null && fields.isArray()) {
            int index = 0;
            for (JsonNode field : fields) {
                String column = index < columns.size() ? columns.get(index) : "col_" + index;
                normalized.put(column, objectMapper.convertValue(field, Object.class));
                index++;
            }
            return normalized;
        }
        if (fields != null && fields.isObject()) {
            fields.fields().forEachRemaining(entry ->
                    normalized.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
            return normalized;
        }
        String column = columns.isEmpty() ? "result" : columns.get(0);
        normalized.put(column, fields == null ? null : objectMapper.convertValue(fields, Object.class));
        return normalized;
    }

    /**
     * 保留已有文本值，缺省时读取JSON字段
     *
     * @param existing 已有文本
     * @param node     JSON节点
     * @param field    字段名
     * @return 文本值
     */
    private String firstText(String existing, JsonNode node, String field) {
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return node.path(field).asText("");
    }

    /**
     * 补充结果行格式参数
     *
     * @param nextResultUri 下一页结果URI
     * @return 带rowFormat参数的URI
     */
    private String appendRowFormat(String nextResultUri) {
        if (nextResultUri.contains("rowFormat=")) {
            return nextResultUri;
        }
        return nextResultUri + (nextResultUri.contains("?") ? "&" : "?") + "rowFormat=JSON";
    }

    /**
     * 判断SQL是否是查询语句
     *
     * @param statement SQL语句
     * @return 是否查询语句
     */
    private boolean isQueryStatement(String statement) {
        String normalized = stripLeadingComments(statement).trim().toUpperCase();
        return normalized.startsWith("SELECT")
                || normalized.startsWith("WITH")
                || normalized.startsWith("SHOW")
                || normalized.startsWith("DESCRIBE")
                || normalized.startsWith("DESC")
                || normalized.startsWith("EXPLAIN");
    }

    /**
     * 拆分多语句SQL
     *
     * @param sql SQL文本
     * @return SQL语句列表
     */
    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !doubleQuote && !backtick) {
                singleQuote = !singleQuote;
            } else if (c == '"' && !singleQuote && !backtick) {
                doubleQuote = !doubleQuote;
            } else if (c == '`' && !singleQuote && !doubleQuote) {
                backtick = !backtick;
            }
            if (c == ';' && !singleQuote && !doubleQuote && !backtick) {
                addStatement(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addStatement(statements, current);
        return statements;
    }

    /**
     * 添加非空SQL语句
     *
     * @param statements SQL语句列表
     * @param current    当前SQL片段
     */
    private void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    /**
     * 去掉SQL开头注释
     *
     * @param statement SQL语句
     * @return 去掉开头注释后的SQL
     */
    private String stripLeadingComments(String statement) {
        String value = statement == null ? "" : statement.stripLeading();
        while (value.startsWith("--")) {
            int lineEnd = value.indexOf('\n');
            if (lineEnd < 0) {
                return "";
            }
            value = value.substring(lineEnd + 1).stripLeading();
        }
        return value;
    }

    /**
     * URL编码
     *
     * @param value 原始值
     * @return 编码后的值
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 当前线程休眠
     *
     * @param millis 休眠毫秒数
     */
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SilentException("FlinkSQL结果轮询被中断");
        }
    }

    /**
     * 获取临时执行结果轮询次数
     *
     * @return 轮询次数
     */
    private int getPreviewPollTimes() {
        return Optional.ofNullable(flinkProperties.getRest().getPreviewPollTimes()).orElse(10);
    }

    /**
     * 获取临时执行结果轮询间隔
     *
     * @return 轮询间隔
     */
    private int getPreviewPollIntervalMs() {
        return Optional.ofNullable(flinkProperties.getRest().getPreviewPollIntervalMs()).orElse(500);
    }

    /**
     * 获取临时执行最多读取结果页数
     *
     * @return 最多读取结果页数
     */
    private int getPreviewMaxResultPages() {
        return Optional.ofNullable(flinkProperties.getRest().getPreviewMaxResultPages()).orElse(20);
    }

    /**
     * 获取临时执行最多返回结果行数
     *
     * @return 最多返回结果行数
     */
    private int getPreviewMaxResultRows() {
        return Optional.ofNullable(flinkProperties.getRest().getPreviewMaxResultRows()).orElse(100);
    }

    /**
     * Mock提交Application Mode作业
     *
     * @param jobName 作业名称
     * @param sql     SQL语句
     * @return mock结果JSON
     */
    private String mockSubmitApplication(String jobName, String sql) {
        Map<String, Object> result = Map.of(
                "mock", true,
                "mode", "APPLICATION",
                "jobName", jobName,
                "sql", sql,
                "message", "Flink Application Mode提交能力尚未接入真实集群"
        );
        return JSON.toJSONString(result);
    }

    /**
     * 将名称转换为 RFC 1123 兼容格式（用于 K8s 资源命名）
     *
     * @param name 原始名称
     * @return RFC 1123 兼容名称
     */
    private String toK8sName(String name) {
        String normalized = name.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
        return normalized.isBlank() ? "flink-application" : normalized;
    }

    /**
     * 生成采集链路 FlinkDeployment 名称
     *
     * @param jobKey 作业标识
     * @return FlinkDeployment 名称
     */
    private String toTrackingDeploymentName(String jobKey) {
        return "dataworks-flink-job-" + toK8sName(jobKey);
    }

    /**
     * 打开session响应对象
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenSessionResponse {

        /**
         * session标识
         */
        private String sessionHandle;
    }

    /**
     * 提交语句响应对象
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExecuteStatementResponse {

        /**
         * operation标识
         */
        private String operationHandle;
    }

    /**
     * SQL语句断言工具
     */
    private static class AssertStatements {

        /**
         * 校验SQL语句列表非空
         *
         * @param statements SQL语句列表
         */
        private static void notEmpty(List<String> statements) {
            if (statements == null || statements.isEmpty()) {
                throw new SilentException("FlinkSQL不能为空");
            }
        }
    }
}
