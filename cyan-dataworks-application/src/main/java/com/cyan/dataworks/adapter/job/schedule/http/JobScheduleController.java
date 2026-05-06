package com.cyan.dataworks.adapter.job.schedule.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job.schedule.http.convert.JobScheduleAdapterConvert;
import com.cyan.dataworks.adapter.job.schedule.http.dto.JobScheduleDTO;
import com.cyan.dataworks.application.job.schedule.JobScheduleService;
import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import com.cyan.dataworks.application.job.schedule.cmd.JobScheduleCmd;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 作业调度配置接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/jobs/{jobId}/schedule")
public class JobScheduleController {

    private final JobScheduleService jobScheduleService;

    public JobScheduleController(JobScheduleService jobScheduleService) {
        this.jobScheduleService = jobScheduleService;
    }

    /**
     * 获取作业调度配置
     */
    @GetMapping
    public Response<JobScheduleDTO> findByJobId(@PathVariable String jobId) {
        JobScheduleBO bo = jobScheduleService.findByJobId(jobId);
        if (bo == null) {
            return Response.success(null);
        }
        JobScheduleDTO dto = JobScheduleAdapterConvert.INSTANCE.toJobScheduleDTO(bo);
        return Response.success(dto);
    }

    /**
     * 保存调度配置
     */
    @PutMapping
    public Response<JobScheduleDTO> saveOrUpdate(@PathVariable String jobId,
                                                  @RequestBody @Valid JobScheduleCmd cmd) {
        JobScheduleBO bo = jobScheduleService.saveOrUpdate(jobId, cmd);
        JobScheduleDTO dto = JobScheduleAdapterConvert.INSTANCE.toJobScheduleDTO(bo);
        return Response.success(dto);
    }
}
