package com.cyan.dataworks.infra.remote.airflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Map;

/**
 * Airflow Task Instance API客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "airflow-task-instance-api", contextId = "airflowTaskInstanceApiClient", url = "${airflow.base-url:}")
public interface AirflowTaskInstanceApiClient {

    /** 查询任务实例列表 */
    @GetMapping
    Map<String, Object> listTaskInstances(URI uri,
                                          @RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestParam Map<String, Object> params);

    /** 更新任务实例状态 */
    @PatchMapping(consumes = "application/json")
    Map<String, Object> patchTaskInstance(URI uri,
                                          @RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestParam("update_mask") String updateMask,
                                          @RequestBody Map<String, Object> body);
}
