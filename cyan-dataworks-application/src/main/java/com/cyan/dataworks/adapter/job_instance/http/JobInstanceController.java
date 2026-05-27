package com.cyan.dataworks.adapter.job_instance.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job_instance.http.convert.JobInstanceAdapterConvert;
import com.cyan.dataworks.adapter.job_instance.http.dto.JobInstanceDTO;
import com.cyan.dataworks.adapter.job_instance.http.dto.JobInstanceLogDTO;
import com.cyan.dataworks.application.job_instance.JobInstanceService;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceLogBO;
import com.cyan.dataworks.application.job_instance.cmd.JobPreviewExecuteCmd;
import com.cyan.dataworks.application.job_instance.cmd.JobRunBySchedulerCmd;
import com.cyan.dataworks.domain.job_instance.query.JobInstanceLogQuery;
import com.cyan.dataworks.domain.job_instance.query.JobInstancePageQuery;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.enums.JobLogRole;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业实例接口
 *
 * <p>提供 JobInstance（作业执行实例）的查询、执行、重试、终止能力。
 * 一个 Job 可产生多个 JobInstance，每次手动执行或调度触发均生成一个新实例。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work")
public class JobInstanceController {

    private final JobInstanceService jobInstanceService;

    public JobInstanceController(JobInstanceService jobInstanceService) {
        this.jobInstanceService = jobInstanceService;
    }

    /**
     * 手动执行作业，生成一个实例
     */
    @PostMapping("/jobs/{jobId}/execute")
    public Response<JobInstanceDTO> execute(@PathVariable String jobId) {
        JobInstanceBO bo = jobInstanceService.execute(jobId);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 临时执行作业，不生成正式实例
     */
    @PostMapping("/jobs/execute-preview")
    public Response<JobInstanceDTO> executePreview(@RequestBody @Valid JobPreviewExecuteCmd cmd) {
        JobInstanceBO bo = jobInstanceService.executePreview(cmd, UserContextHolder.getCurrentEmployee().getPassport());
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 启动正式Application Mode作业
     */
    @PostMapping("/jobs/{jobId}/start")
    public Response<JobInstanceDTO> startApplication(@PathVariable String jobId) {
        JobInstanceBO bo = jobInstanceService.startApplication(jobId, UserContextHolder.getCurrentEmployee().getPassport());
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 调度器触发执行作业
     */
    @PostMapping("/jobs/{jobId}/run-by-scheduler")
    public Response<JobInstanceDTO> runByScheduler(@PathVariable String jobId,
                                                   @RequestBody @Valid JobRunBySchedulerCmd cmd) {
        JobInstanceBO bo = jobInstanceService.runByScheduler(jobId, cmd);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 分页查询某个作业下的实例
     */
    @GetMapping("/jobs/{jobId}/instances")
    public Response<Page<JobInstanceDTO>> pageByJobId(@PathVariable String jobId,
                                                       @RequestParam(required = false) ExecutionStatus status,
                                                       @RequestParam(required = false) Long current,
                                                       @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        JobInstancePageQuery query = new JobInstancePageQuery()
                .setJobId(jobId)
                .setStatus(status)
                .setCurrent(current)
                .setSize(size);
        Page<JobInstanceBO> page = jobInstanceService.page(query);
        List<JobInstanceDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(JobInstanceAdapterConvert.INSTANCE::toJobInstanceDTO).toList();
        Page<JobInstanceDTO> result = new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
        return Response.success(result);
    }

    /**
     * 分页查询全部实例
     */
    @GetMapping("/instances")
    public Response<Page<JobInstanceDTO>> page(@RequestParam(required = false) ExecutionStatus status,
                                                @RequestParam(required = false) Long current,
                                                @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        JobInstancePageQuery query = new JobInstancePageQuery()
                .setStatus(status)
                .setCurrent(current)
                .setSize(size);
        Page<JobInstanceBO> page = jobInstanceService.page(query);
        List<JobInstanceDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(JobInstanceAdapterConvert.INSTANCE::toJobInstanceDTO).toList();
        Page<JobInstanceDTO> result = new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
        return Response.success(result);
    }

    /**
     * 根据ID查询实例
     */
    @GetMapping("/instances/{id}")
    public Response<JobInstanceDTO> findById(@PathVariable String id) {
        JobInstanceBO bo = jobInstanceService.findById(id);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 查询实例K8s Pod日志
     */
    @GetMapping("/instances/{id}/logs")
    public Response<JobInstanceLogDTO> getLogs(@PathVariable String id,
                                               @RequestParam(required = false) JobLogRole role,
                                               @RequestParam(required = false) Integer tailLines,
                                               @RequestParam(required = false) Boolean previous) {
        JobInstanceLogQuery query = new JobInstanceLogQuery()
                .setRole(role == null ? JobLogRole.ALL : role)
                .setTailLines(tailLines)
                .setPrevious(previous == null ? Boolean.FALSE : previous);
        JobInstanceLogBO bo = jobInstanceService.getLogs(id, query);
        JobInstanceLogDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceLogDTO(bo);
        return Response.success(dto);
    }

    /**
     * 重试实例
     */
    @PostMapping("/instances/{id}/retry")
    public Response<JobInstanceDTO> retry(@PathVariable String id) {
        JobInstanceBO bo = jobInstanceService.retry(id);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }

    /**
     * 终止实例
     */
    @PostMapping("/instances/{id}/terminate")
    public Response<JobInstanceDTO> terminate(@PathVariable String id) {
        JobInstanceBO bo = jobInstanceService.terminate(id);
        JobInstanceDTO dto = JobInstanceAdapterConvert.INSTANCE.toJobInstanceDTO(bo);
        return Response.success(dto);
    }
}
