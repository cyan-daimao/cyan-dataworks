package com.cyan.dataworks.adapter.job_instance.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job_instance.http.convert.JobInstanceAdapterConvert;
import com.cyan.dataworks.application.job_instance.JobInstanceService;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.cmd.JobInstanceCallbackCmd;
import com.cyan.dataworks.application.job_instance.cmd.JobRunBySchedulerCmd;
import com.cyan.dataworks.client.job_instance.DataWorksRpcJobInstanceClient;
import com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO;
import com.cyan.dataworks.client.job_instance.request.JobInstanceCallbackRequest;
import com.cyan.dataworks.client.job_instance.request.JobRunBySchedulerRequest;
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
public class DataWorksJobInstanceRpcController implements DataWorksRpcJobInstanceClient {

    private final JobInstanceService jobInstanceService;

    public DataWorksJobInstanceRpcController(JobInstanceService jobInstanceService) {
        this.jobInstanceService = jobInstanceService;
    }

    /**
     * 启动正式 Application Mode 作业
     */
    @PostMapping("/{jobId}/start-application")
    @Override
    public Response<JobInstanceDTO> startApplication(@PathVariable String jobId,
                                                      @RequestParam(required = false) String createdBy) {
        String operator = createdBy != null && !createdBy.isBlank() ? createdBy : "system";
        JobInstanceBO bo = jobInstanceService.startApplication(jobId, operator);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toRpcJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 调度器触发执行作业
     *
     * @param jobId 作业ID
     * @return 作业实例
     */
    @PostMapping("/{jobId}/run-by-scheduler")
    @Override
    public Response<JobInstanceDTO> runByScheduler(@PathVariable String jobId,
                                                    @RequestBody @Valid JobRunBySchedulerRequest request) {
        JobRunBySchedulerCmd cmd = JobInstanceAdapterConvert.INSTANCE.toJobRunBySchedulerCmd(request);
        JobInstanceBO bo = jobInstanceService.runByScheduler(jobId, cmd);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toRpcJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 根据 ID 查询实例
     */
    @GetMapping("/{instanceId}")
    @Override
    public Response<JobInstanceDTO> findById(@PathVariable String instanceId) {
        JobInstanceBO bo = jobInstanceService.findById(instanceId);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toRpcJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 查询调度器等待状态
     */
    @GetMapping("/{instanceId}/scheduler-status")
    @Override
    public Response<JobInstanceDTO> findSchedulerStatus(@PathVariable String instanceId) {
        JobInstanceBO bo = jobInstanceService.findSchedulerStatus(instanceId);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toRpcJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * Pod执行完成回调
     */
    @PostMapping("/{instanceId}/callback")
    @Override
    public Response<JobInstanceDTO> callback(@PathVariable String instanceId,
                                             @RequestHeader("X-DataWorks-Callback-Token") String callbackToken,
                                             @RequestBody @Valid JobInstanceCallbackRequest request) {
        JobInstanceCallbackCmd cmd = JobInstanceAdapterConvert.INSTANCE.toJobInstanceCallbackCmd(request);
        JobInstanceBO bo = jobInstanceService.callback(instanceId, cmd, callbackToken);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toRpcJobInstanceDTO(bo);
        return Response.success(dto);
    }
}
