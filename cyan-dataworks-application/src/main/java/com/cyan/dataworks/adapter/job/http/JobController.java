package com.cyan.dataworks.adapter.job.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job.http.convert.JobAdapterConvert;
import com.cyan.dataworks.adapter.job.http.dto.JobDTO;
import com.cyan.dataworks.application.job.JobService;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.domain.job.query.JobPageQuery;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工作业接口
 *
 * <p>提供 Job（作业定义）的 CRUD 和分页查询能力。执行能力在 {@link com.cyan.dataworks.adapter.job_instance.http.JobInstanceController}。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * 分页查询作业
     */
    @GetMapping
    public Response<Page<JobDTO>> page(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) EngineType engineType,
                                        @RequestParam(required = false) Long folderId,
                                        @RequestParam(required = false) Long current,
                                        @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        JobPageQuery query = new JobPageQuery()
                .setName(name)
                .setEngineType(engineType)
                .setFolderId(folderId)
                .setCreatedBy(UserContextHolder.getCurrentEmployee().getPassport());
        query.setCurrent(current).setSize(size);
        Page<JobBO> page = jobService.page(query);
        List<JobDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(JobAdapterConvert.INSTANCE::toJobDTO).toList();
        Page<JobDTO> result = new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
        return Response.success(result);
    }

    /**
     * 根据ID查询作业
     */
    @GetMapping("/{id}")
    public Response<JobDTO> findById(@PathVariable String id) {
        JobBO bo = jobService.findById(id);
        JobDTO dto = JobAdapterConvert.INSTANCE.toJobDTO(bo);
        return Response.success(dto);
    }

    /**
     * 保存作业
     */
    @PostMapping
    public Response<JobDTO> save(@RequestBody @Valid JobCmd cmd) {
        JobBO bo = jobService.save(cmd, UserContextHolder.getCurrentEmployee().getPassport());
        JobDTO dto = JobAdapterConvert.INSTANCE.toJobDTO(bo);
        return Response.success(dto);
    }

    /**
     * 更新作业
     */
    @PutMapping("/{id}")
    public Response<JobDTO> update(@PathVariable String id, @RequestBody @Valid JobCmd cmd) {
        JobBO bo = jobService.update(id, cmd, UserContextHolder.getCurrentEmployee().getPassport());
        JobDTO dto = JobAdapterConvert.INSTANCE.toJobDTO(bo);
        return Response.success(dto);
    }

    /**
     * 删除作业
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        jobService.delete(id);
        return Response.success();
    }
}
