package com.cyan.dataworks.client.job_instance;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO;
import com.cyan.dataworks.client.job_instance.request.JobInstanceCallbackRequest;
import com.cyan.dataworks.client.job_instance.request.JobRunBySchedulerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
     * 调度器触发执行作业
     *
     * @param jobId 作业ID
     * @param request 调度器执行请求
     * @return 作业实例
     */
    @PostMapping("/{jobId}/run-by-scheduler")
    Response<JobInstanceDTO> runByScheduler(@PathVariable String jobId,
                                            @RequestBody JobRunBySchedulerRequest request);

    /**
     * 根据 ID 查询实例
     */
    @GetMapping("/{instanceId}")
    Response<JobInstanceDTO> findById(@PathVariable String instanceId);

    /**
     * 查询调度器等待状态
     */
    @GetMapping("/{instanceId}/scheduler-status")
    Response<JobInstanceDTO> findSchedulerStatus(@PathVariable String instanceId);

    /**
     * Pod执行完成回调
     */
    @PostMapping("/{instanceId}/callback")
    Response<JobInstanceDTO> callback(@PathVariable String instanceId,
                                      @RequestHeader("X-DataWorks-Callback-Token") String callbackToken,
                                      @RequestBody JobInstanceCallbackRequest request);
}
