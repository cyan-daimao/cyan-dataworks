package com.cyan.dataworks.infra.remote.flink.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.Map;

/**
 * Flink SQL Gateway REST API Feign 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "flink-gateway", contextId = "flinkRpcClient", url = "${feign.flink-gateway.url:}")
public interface FlinkRpcClient {

    /**
     * 通用 POST 请求
     *
     * @param uri  请求地址
     * @param body 请求体
     * @return 响应体
     */
    @PostMapping
    String post(URI uri, @RequestBody Map<String, Object> body);

    /**
     * 通用 GET 请求
     *
     * @param uri 请求地址
     * @return 响应体
     */
    @GetMapping
    String get(URI uri);

    /**
     * 通用 DELETE 请求
     *
     * @param uri  请求地址
     * @param body 请求体
     * @return 响应体
     */
    @DeleteMapping
    String delete(URI uri, @RequestBody Map<String, Object> body);
}
