package com.cyan.dataworks.infra.rpc;

import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.JSON;
import com.cyan.dataworks.infra.config.FlinkProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Flink SQL Gateway REST API 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class FlinkRpcClient {

    /**
     * HTTP客户端
     */
    private final RestTemplate restTemplate;

    /**
     * JSON解析器
     */
    private final ObjectMapper objectMapper;

    /**
     * Flink配置属性
     */
    private final FlinkProperties flinkProperties;

    /**
     * 创建Flink SQL Gateway客户端
     *
     * @param objectMapper JSON解析器
     * @param flinkProperties Flink配置属性
     */
    public FlinkRpcClient(ObjectMapper objectMapper, FlinkProperties flinkProperties) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.flinkProperties = flinkProperties;
    }

    /**
     * 通过SQL Gateway临时执行FlinkSQL
     *
     * @param sql SQL语句
     * @return 执行结果JSON
     */
    public String executeSql(String sql) {
        String gatewayUrl = getGatewayUrl();
        if (gatewayUrl == null || gatewayUrl.isEmpty()) {
            log.warn("Flink REST URL未配置，返回mock结果");
            return mockExecuteSql(sql);
        }

        String sessionHandle = null;
        String operationHandle = null;
        try {
            sessionHandle = openSession(gatewayUrl);
            List<String> statements = splitStatements(sql);
            AssertStatements.notEmpty(statements);
            List<Map<String, Object>> executedStatements = new ArrayList<>();
            String lastResult = "{}";
            for (int i = 0; i < statements.size(); i++) {
                boolean last = i == statements.size() - 1;
                String statement = statements.get(i);
                operationHandle = executeStatement(gatewayUrl, sessionHandle, statement);
                lastResult = fetchOperationResult(gatewayUrl, sessionHandle, operationHandle, statement);
                executedStatements.add(Map.of(
                        "statement", statement,
                        "operationHandle", operationHandle,
                        "result", parseJsonOrRaw(lastResult)
                ));
                if (!last) {
                    closeOperation(gatewayUrl, sessionHandle, operationHandle);
                    operationHandle = null;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mock", false);
            result.put("mode", "SESSION");
            result.put("sessionHandle", sessionHandle);
            result.put("operationHandle", operationHandle);
            result.put("result", parseJsonOrRaw(lastResult));
            result.put("statements", executedStatements);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("FlinkSQL临时执行失败", e);
            throw new SilentException("FlinkSQL临时执行失败：" + e.getMessage());
        } finally {
            closeQuietly(gatewayUrl, sessionHandle, operationHandle);
        }
    }

    /**
     * 以Application Mode提交FlinkSQL正式作业
     *
     * @param jobName 作业名称
     * @param sql SQL语句
     * @return 提交结果JSON
     */
    public String submitApplication(String jobName, String sql) {
        if (getGatewayUrl() == null || getGatewayUrl().isEmpty()) {
            log.warn("Flink REST URL未配置，返回Application Mode mock结果");
            return mockSubmitApplication(jobName, sql);
        }
        // TODO 对接 Flink Kubernetes Operator 或 application cluster 提交流程。
        log.warn("Application Mode提交尚未接入真实集群，返回mock结果");
        return mockSubmitApplication(jobName, sql);
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
     * @param gatewayUrl SQL Gateway地址
     * @return sessionHandle
     */
    private String openSession(String gatewayUrl) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                gatewayUrl + "/v1/sessions",
                jsonEntity(Map.of()),
                String.class
        );
        OpenSessionResponse body = readResponse(response.getBody(), OpenSessionResponse.class);
        if (body == null || body.getSessionHandle() == null || body.getSessionHandle().isBlank()) {
            throw new SilentException("SQL Gateway打开session失败");
        }
        return body.getSessionHandle();
    }

    /**
     * 执行单条SQL语句
     *
     * @param gatewayUrl SQL Gateway地址
     * @param sessionHandle session标识
     * @param statement SQL语句
     * @return operationHandle
     */
    private String executeStatement(String gatewayUrl, String sessionHandle, String statement) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statement", statement);
        ResponseEntity<String> response = restTemplate.postForEntity(
                gatewayUrl + "/v1/sessions/" + encode(sessionHandle) + "/statements",
                jsonEntity(body),
                String.class
        );
        ExecuteStatementResponse result = readResponse(response.getBody(), ExecuteStatementResponse.class);
        if (result == null || result.getOperationHandle() == null || result.getOperationHandle().isBlank()) {
            throw new SilentException("SQL Gateway提交SQL失败");
        }
        return result.getOperationHandle();
    }

    /**
     * 拉取operation执行结果
     *
     * @param gatewayUrl SQL Gateway地址
     * @param sessionHandle session标识
     * @param operationHandle operation标识
     * @param statement SQL语句
     * @return operation结果JSON
     */
    private String fetchOperationResult(String gatewayUrl, String sessionHandle, String operationHandle, String statement) {
        if (!isQueryStatement(statement)) {
            return getOperationStatus(gatewayUrl, sessionHandle, operationHandle);
        }
        String resultUri = "/v1/sessions/" + encode(sessionHandle)
                + "/operations/" + encode(operationHandle)
                + "/result/0?rowFormat=JSON";
        List<Object> data = new ArrayList<>();
        Object columns = List.of();
        List<Object> pages = new ArrayList<>();
        String resultKind = "";
        String jobId = "";
        int readPages = 0;
        while (readPages < getPreviewMaxResultPages() && data.size() < getPreviewMaxResultRows()) {
            String body = fetchReadyResultPage(gatewayUrl, resultUri);
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
                    columns = objectMapper.convertValue(results.get("columns"), Object.class);
                }
                JsonNode rows = results.get("data");
                if (rows != null && rows.isArray()) {
                    for (JsonNode row : rows) {
                        if (data.size() >= getPreviewMaxResultRows()) {
                            break;
                        }
                        data.add(objectMapper.convertValue(row, Object.class));
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
        result.put("maxRowsReached", data.size() >= getPreviewMaxResultRows());
        result.put("results", Map.of(
                "columns", columns,
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
     * @param gatewayUrl SQL Gateway地址
     * @param resultUri 结果页相对路径
     * @return 结果页JSON
     */
    private String fetchReadyResultPage(String gatewayUrl, String resultUri) {
        String resultUrl = gatewayUrl + resultUri;
        int pollTimes = getPreviewPollTimes();
        for (int i = 0; i < pollTimes; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(resultUrl, String.class);
            String body = response.getBody();
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
     * @param gatewayUrl SQL Gateway地址
     * @param sessionHandle session标识
     * @param operationHandle operation标识
     * @return operation状态JSON
     */
    private String getOperationStatus(String gatewayUrl, String sessionHandle, String operationHandle) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/v1/sessions/" + encode(sessionHandle)
                        + "/operations/" + encode(operationHandle) + "/status",
                String.class
        );
        return response.getBody();
    }

    /**
     * 关闭operation
     *
     * @param gatewayUrl SQL Gateway地址
     * @param sessionHandle session标识
     * @param operationHandle operation标识
     */
    private void closeOperation(String gatewayUrl, String sessionHandle, String operationHandle) {
        restTemplate.exchange(
                gatewayUrl + "/v1/sessions/" + encode(sessionHandle)
                        + "/operations/" + encode(operationHandle) + "/close",
                HttpMethod.DELETE,
                jsonEntity(Map.of()),
                String.class
        );
    }

    /**
     * 关闭session
     *
     * @param gatewayUrl SQL Gateway地址
     * @param sessionHandle session标识
     */
    private void closeSession(String gatewayUrl, String sessionHandle) {
        restTemplate.exchange(
                gatewayUrl + "/v1/sessions/" + encode(sessionHandle),
                HttpMethod.DELETE,
                jsonEntity(Map.of()),
                String.class
        );
    }

    /**
     * 安静关闭SQL Gateway资源
     *
     * @param gatewayUrl SQL Gateway地址
     * @param sessionHandle session标识
     * @param operationHandle operation标识
     */
    private void closeQuietly(String gatewayUrl, String sessionHandle, String operationHandle) {
        try {
            if (gatewayUrl != null && !gatewayUrl.isBlank() && sessionHandle != null && operationHandle != null) {
                closeOperation(gatewayUrl, sessionHandle, operationHandle);
            }
        } catch (Exception e) {
            log.warn("关闭Flink SQL Gateway operation失败: {}", e.getMessage());
        }
        try {
            if (gatewayUrl != null && !gatewayUrl.isBlank() && sessionHandle != null) {
                closeSession(gatewayUrl, sessionHandle);
            }
        } catch (Exception e) {
            log.warn("关闭Flink SQL Gateway session失败: {}", e.getMessage());
        }
    }

    /**
     * 构造JSON请求体
     *
     * @param body 请求体
     * @return HTTP请求实体
     */
    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 读取JSON响应
     *
     * @param body 响应体
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
     * 保留已有文本值，缺省时读取JSON字段
     *
     * @param existing 已有文本
     * @param node JSON节点
     * @param field 字段名
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
     * @param current 当前SQL片段
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
     * @param sql SQL语句
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
