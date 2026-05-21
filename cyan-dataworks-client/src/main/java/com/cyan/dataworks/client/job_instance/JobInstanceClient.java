package com.cyan.dataworks.client.job_instance;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO;
import com.cyan.dataworks.client.job_instance.query.JobInstancePageQuery;
import com.cyan.dataworks.client.job_instance.request.JobPreviewExecuteRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

/**
 * 数据加工作业实例 Feign 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "jobInstanceClient", path = "/api/v1/data-work", url = "${feign.cyan-dataworks.url:}")
public interface JobInstanceClient {

    /**
     * 手动执行作业，生成一个实例
     */
    @PostMapping("/jobs/{jobId}/execute")
    Response<JobInstanceDTO> execute(@PathVariable String jobId);

    /**
     * 临时执行作业，不生成正式实例
     */
    @PostMapping("/jobs/execute-preview")
    Response<JobInstanceDTO> executePreview(@RequestBody JobPreviewExecuteRequest request);

    /**
     * 启动正式Application Mode作业
     */
    @PostMapping("/jobs/{jobId}/start")
    Response<JobInstanceDTO> startApplication(@PathVariable String jobId);

    /**
     * 分页查询某个作业下的实例
     */
    @GetMapping("/jobs/{jobId}/instances")
    Response<Page<JobInstanceDTO>> pageByJobId(@PathVariable String jobId, @SpringQueryMap JobInstancePageQuery query);

    /**
     * 分页查询全部实例
     */
    @GetMapping("/instances")
    Response<Page<JobInstanceDTO>> page(@SpringQueryMap JobInstancePageQuery query);

    /**
     * 根据ID查询实例
     */
    @GetMapping("/instances/{id}")
    Response<JobInstanceDTO> findById(@PathVariable String id);

    /**
     * 重试实例
     */
    @PostMapping("/instances/{id}/retry")
    Response<JobInstanceDTO> retry(@PathVariable String id);

    /**
     * 终止实例
     */
    @PostMapping("/instances/{id}/terminate")
    Response<JobInstanceDTO> terminate(@PathVariable String id);
}
