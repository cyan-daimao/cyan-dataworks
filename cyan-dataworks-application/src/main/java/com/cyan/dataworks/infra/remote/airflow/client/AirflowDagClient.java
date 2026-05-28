package com.cyan.dataworks.infra.remote.airflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Map;

/**
 * Airflow DAG REST API Feign客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "airflow-dag", contextId = "airflowDagClient", url = "${airflow.base-url:}")
public interface AirflowDagClient {

    /**
     * 更新DAG状态
     *
     * @param uri           请求地址
     * @param authorization 认证信息
     * @param updateMask    更新字段
     * @param body          请求体
     */
    @PatchMapping(consumes = "application/json")
    void patchDag(URI uri,
                  @RequestHeader(value = "Authorization", required = false) String authorization,
                  @RequestParam("update_mask") String updateMask,
                  @RequestBody Map<String, Object> body);
}
