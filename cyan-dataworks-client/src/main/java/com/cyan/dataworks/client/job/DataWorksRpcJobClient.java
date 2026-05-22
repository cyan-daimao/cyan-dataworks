package com.cyan.dataworks.client.job;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job.request.JobSaveRequest;
import com.cyan.dataworks.client.job.dto.JobDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * DataWorks Job RPC Feign 客户端
 * <p>
 * 供其他微服务调用，路径为 /rpc/dataworks，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "dataWorksRpcJobClient", path = "/rpc/dataworks/jobs", url = "${feign.cyan-dataworks.url:}")
public interface DataWorksRpcJobClient {

    /**
     * 保存作业（创建）
     */
    @PostMapping
    Response<JobDTO> save(@RequestBody JobSaveRequest request);

    /**
     * 发布作业
     */
    @PostMapping("/{id}/publish")
    Response<JobDTO> publish(@PathVariable String id);
}
