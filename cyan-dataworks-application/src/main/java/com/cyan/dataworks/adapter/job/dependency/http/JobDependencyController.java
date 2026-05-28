package com.cyan.dataworks.adapter.job.dependency.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job.dependency.http.convert.JobDependencyAdapterConvert;
import com.cyan.dataworks.adapter.job.dependency.http.dto.JobDependencyDTO;
import com.cyan.dataworks.adapter.job.dependency.http.dto.JobLineageDTO;
import com.cyan.dataworks.application.job.dependency.JobDependencyService;
import com.cyan.dataworks.application.job.dependency.bo.JobDependencyBO;
import com.cyan.dataworks.application.job.dependency.bo.JobLineageBO;
import com.cyan.dataworks.application.job.dependency.cmd.JobDependencyCmd;
import com.cyan.employee.login.filter.UserContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 作业依赖接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/jobs/{jobId}")
public class JobDependencyController {

    /**
     * 作业依赖应用服务
     */
    private final JobDependencyService jobDependencyService;

    public JobDependencyController(JobDependencyService jobDependencyService) {
        this.jobDependencyService = jobDependencyService;
    }

    /**
     * 查询作业依赖
     */
    @GetMapping("/dependencies")
    public Response<JobDependencyDTO> findByJobId(@PathVariable String jobId) {
        JobDependencyBO bo = jobDependencyService.findByJobId(jobId);
        return Response.success(JobDependencyAdapterConvert.INSTANCE.toJobDependencyDTO(bo));
    }

    /**
     * 保存作业依赖
     */
    @PutMapping("/dependencies")
    public Response<JobDependencyDTO> save(@PathVariable String jobId,
                                           @RequestBody JobDependencyCmd cmd) {
        JobDependencyBO bo = jobDependencyService.save(jobId, cmd, UserContextHolder.getCurrentEmployee().getPassport());
        return Response.success(JobDependencyAdapterConvert.INSTANCE.toJobDependencyDTO(bo));
    }

    /**
     * 查询作业血缘
     */
    @GetMapping("/lineage")
    public Response<JobLineageDTO> lineage(@PathVariable String jobId) {
        JobLineageBO bo = jobDependencyService.lineage(jobId);
        return Response.success(JobDependencyAdapterConvert.INSTANCE.toJobLineageDTO(bo));
    }
}
