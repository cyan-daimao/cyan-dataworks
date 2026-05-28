package com.cyan.dataworks.infra.remote.airflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;
import java.util.Map;

/**
 * Airflow Task清理API客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "airflow-task-clear-api", contextId = "airflowTaskClearApiClient", url = "${airflow.base-url:}")
public interface AirflowTaskClearApiClient {

    /** 清理任务实例以触发重跑 */
    @PostMapping(consumes = "application/json")
    Map<String, Object> clearTaskInstances(URI uri,
                                           @RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody Map<String, Object> body);
}
