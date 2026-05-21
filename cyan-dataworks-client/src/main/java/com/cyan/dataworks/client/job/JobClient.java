package com.cyan.dataworks.client.job;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job.dto.JobDTO;
import com.cyan.dataworks.client.job.query.JobPageQuery;
import com.cyan.dataworks.client.job.request.JobSaveRequest;
import com.cyan.dataworks.client.job.request.JobUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

/**
 * 数据加工作业 Feign 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "jobClient", path = "/api/v1/data-work/jobs", url = "${feign.cyan-dataworks.url:}")
public interface JobClient {

    /**
     * 分页查询作业
     */
    @GetMapping
    Response<Page<JobDTO>> page(@SpringQueryMap JobPageQuery query);

    /**
     * 根据ID查询作业
     */
    @GetMapping("/{id}")
    Response<JobDTO> findById(@PathVariable String id);

    /**
     * 保存作业
     */
    @PostMapping
    Response<JobDTO> save(@RequestBody JobSaveRequest request);

    /**
     * 更新作业
     */
    @PutMapping("/{id}")
    Response<JobDTO> update(@PathVariable String id, @RequestBody JobUpdateRequest request);

    /**
     * 删除作业
     */
    @DeleteMapping("/{id}")
    Response<Void> delete(@PathVariable String id);

    /**
     * 发布作业
     */
    @PutMapping("/{id}/publish")
    Response<JobDTO> publish(@PathVariable String id);

    /**
     * 下线作业
     */
    @PutMapping("/{id}/offline")
    Response<JobDTO> offline(@PathVariable String id);
}
