package com.cyan.dataworks.infra.remote.airflow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Map;

/**
 * Airflow DAG API客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "dataworks-airflow-dag-api", contextId = "dataworksAirflowDagApiClient", url = "${airflow.base-url:}")
public interface AirflowDagApiClient {

    /** 查询DAG列表 */
    @GetMapping
    Map<String, Object> listDags(URI uri,
                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, Object> params);

    /** 查询DAG详情 */
    @GetMapping
    Map<String, Object> getDag(URI uri,
                               @RequestHeader(value = "Authorization", required = false) String authorization);

    /** 更新DAG */
    @PatchMapping(consumes = "application/json")
    Map<String, Object> patchDag(URI uri,
                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam("update_mask") String updateMask,
                                 @RequestBody Map<String, Object> body);

    /** 删除DAG */
    @DeleteMapping
    void deleteDag(URI uri,
                   @RequestHeader(value = "Authorization", required = false) String authorization);
}
