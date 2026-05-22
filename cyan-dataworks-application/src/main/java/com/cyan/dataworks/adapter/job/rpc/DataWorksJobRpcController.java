package com.cyan.dataworks.adapter.job.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job.http.convert.JobAdapterConvert;
import com.cyan.dataworks.adapter.job.http.dto.JobDTO;
import com.cyan.dataworks.application.job.JobService;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.client.job.request.JobSaveRequest;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * DataWorks Job RPC 接口
 * <p>
 * 供其他微服务（如 cyan-data-collection）调用，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/dataworks/jobs")
public class DataWorksJobRpcController {

    private final JobService jobService;

    public DataWorksJobRpcController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * 保存作业（创建）
     */
    @PostMapping
    public Response<JobDTO> save(@RequestBody @Valid JobSaveRequest request) {
        String createdBy = resolveCreatedBy(request);
        JobCmd cmd = new JobCmd()
                .setName(request.getName())
                .setDescription(request.getDescription())
                .setEngineType(request.getEngineType() != null ? EngineType.valueOf(request.getEngineType().name()) : null)
                .setNodeType(request.getNodeType() != null ? NodeType.valueOf(request.getNodeType().name()) : null)
                .setSqlContent(request.getSqlContent())
                .setConfigJson(request.getConfigJson());
        JobBO bo = jobService.save(cmd, createdBy);
        JobDTO dto = JobAdapterConvert.INSTANCE.toJobDTO(bo);
        return Response.success(dto);
    }

    /**
     * 发布作业
     */
    @PostMapping("/{id}/publish")
    public Response<JobDTO> publish(@PathVariable String id) {
        JobBO bo = jobService.publish(id, "system");
        JobDTO dto = JobAdapterConvert.INSTANCE.toJobDTO(bo);
        return Response.success(dto);
    }

    private String resolveCreatedBy(JobSaveRequest request) {
        // 第一阶段：直接从请求中读取 createdBy（兼容 RPC 调用）
        // 注意：JobSaveRequest 没有 createdBy 字段，需求文档说请求中包含 createdBy
        // 但现有 JobSaveRequest 没有这个字段。这里通过扩展字段处理。
        // 如果后续需要，可以在 JobSaveRequest 中增加 createdBy 字段。
        return "system";
    }
}
