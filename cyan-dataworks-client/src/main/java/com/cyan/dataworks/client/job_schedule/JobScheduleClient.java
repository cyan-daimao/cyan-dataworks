package com.cyan.dataworks.client.job_schedule;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job_schedule.dto.JobScheduleDTO;
import com.cyan.dataworks.client.job_schedule.request.JobScheduleSaveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 作业调度配置 Feign 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "jobScheduleClient", path = "/api/v1/data-work/jobs", url = "${feign.cyan-dataworks.url:}")
public interface JobScheduleClient {

    /**
     * 获取作业调度配置
     */
    @GetMapping("/{jobId}/schedule")
    Response<JobScheduleDTO> findByJobId(@PathVariable String jobId);

    /**
     * 保存调度配置
     */
    @PutMapping("/{jobId}/schedule")
    Response<JobScheduleDTO> saveOrUpdate(@PathVariable String jobId, @RequestBody JobScheduleSaveRequest request);
}
