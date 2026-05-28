package com.cyan.dataworks.infra.remote.airflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Map;

/**
 * Airflow DAG Run API客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "dataworks-airflow-dag-run-api", contextId = "dataworksAirflowDagRunApiClient", url = "${airflow.base-url:}")
public interface AirflowDagRunApiClient {

    /** 触发DAG Run */
    @PostMapping(consumes = "application/json")
    Map<String, Object> triggerDagRun(URI uri,
                                      @RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody Map<String, Object> body);

    /** 查询DAG Run列表 */
    @GetMapping
    Map<String, Object> listDagRuns(URI uri,
                                    @RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestParam Map<String, Object> params);

    /** 查询DAG Run详情 */
    @GetMapping
    Map<String, Object> getDagRun(URI uri,
                                  @RequestHeader(value = "Authorization", required = false) String authorization);

    /** 更新DAG Run状态 */
    @PatchMapping(consumes = "application/json")
    Map<String, Object> patchDagRun(URI uri,
                                    @RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestParam("update_mask") String updateMask,
                                    @RequestBody Map<String, Object> body);
}
