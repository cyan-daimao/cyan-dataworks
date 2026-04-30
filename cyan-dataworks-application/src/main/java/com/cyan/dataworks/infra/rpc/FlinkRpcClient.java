package com.cyan.dataworks.infra.rpc;

import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Flink REST API 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class FlinkRpcClient {

    private final RestTemplate restTemplate;

    @Value("${flink.rest.url:}")
    private String flinkRestUrl;

    public FlinkRpcClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 执行FlinkSQL
     *
     * @param sql SQL语句
     * @return 执行结果JSON
     */
    public String executeSql(String sql) {
        if (flinkRestUrl == null || flinkRestUrl.isEmpty()) {
            log.warn("Flink REST URL未配置，返回mock结果");
            return mockExecuteSql(sql);
        }

        try {
            String url = flinkRestUrl + "/v1/sql/statements";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("statement", sql);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("FlinkSQL执行失败，返回mock结果", e);
            return mockExecuteSql(sql);
        }
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
                "sql", sql,
                "message", "Flink集群未连接，返回mock结果"
        );
        return JSON.toJSONString(result);
    }
}
