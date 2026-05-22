package com.cyan.dataworks.client.job_instance;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * DataWorks JobInstance RPC Feign 客户端
 * <p>
 * 供其他微服务调用，路径为 /rpc/dataworks，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "dataWorksRpcJobInstanceClient", path = "/rpc/dataworks/job-instances", url = "${feign.cyan-dataworks.url:}")
public interface DataWorksRpcJobInstanceClient {

    /**
     * 启动正式 Application Mode 作业
     */
    @PostMapping("/{jobId}/start-application")
    Response<JobInstanceDTO> startApplication(@PathVariable String jobId,
                                               @RequestParam(required = false) String createdBy);

    /**
     * 根据 ID 查询实例
     */
    @GetMapping("/{instanceId}")
    Response<JobInstanceDTO> findById(@PathVariable String instanceId);
}
