package com.cyan.dataworks.adapter.job_instance.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job_instance.http.convert.JobInstanceAdapterConvert;
import com.cyan.dataworks.adapter.job_instance.http.dto.JobInstanceDTO;
import com.cyan.dataworks.application.job_instance.JobInstanceService;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.cmd.JobRunBySchedulerCmd;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * DataWorks JobInstance RPC 接口
 * <p>
 * 供其他微服务（如 cyan-data-collection）调用，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/dataworks/job-instances")
public class DataWorksJobInstanceRpcController {

    private final JobInstanceService jobInstanceService;

    public DataWorksJobInstanceRpcController(JobInstanceService jobInstanceService) {
        this.jobInstanceService = jobInstanceService;
    }

    /**
     * 启动正式 Application Mode 作业
     */
    @PostMapping("/{jobId}/start-application")
    public Response<JobInstanceDTO> startApplication(@PathVariable String jobId,
                                                      @RequestParam(required = false) String createdBy) {
        String operator = createdBy != null && !createdBy.isBlank() ? createdBy : "system";
        JobInstanceBO bo = jobInstanceService.startApplication(jobId, operator);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 调度器触发执行作业
     *
     * @param jobId 作业ID
     * @param cmd 调度器执行命令
     * @return 作业实例
     */
    @PostMapping("/{jobId}/run-by-scheduler")
    public Response<JobInstanceDTO> runByScheduler(@PathVariable String jobId,
                                                    @RequestBody @Valid JobRunBySchedulerCmd cmd) {
        JobInstanceBO bo = jobInstanceService.runByScheduler(jobId, cmd);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 根据 ID 查询实例
     */
    @GetMapping("/{instanceId}")
    public Response<JobInstanceDTO> findById(@PathVariable String instanceId) {
        JobInstanceBO bo = jobInstanceService.findById(instanceId);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }
}
