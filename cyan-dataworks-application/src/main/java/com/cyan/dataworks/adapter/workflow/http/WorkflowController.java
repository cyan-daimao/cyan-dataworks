package com.cyan.dataworks.adapter.workflow.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.workflow.http.convert.WorkflowAdapterConvert;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowDefinitionDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowDependencyDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowScheduleDTO;
import com.cyan.dataworks.application.workflow.WorkflowService;
import com.cyan.dataworks.application.workflow.bo.WorkflowBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowDefinitionCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowDependencyCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowScheduleCmd;
import com.cyan.dataworks.domain.workflow.query.WorkflowPageQuery;
import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 工作流管理接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/workflows")
public class WorkflowController {

    /** 工作流应用服务 */
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /** 分页查询工作流 */
    @GetMapping
    public Response<Page<WorkflowDTO>> page(@RequestParam(required = false) String name,
                                            @RequestParam(required = false) TaskStatus status,
                                            @RequestParam(required = false) Long current,
                                            @RequestParam(required = false) Long size) {
        WorkflowPageQuery query = new WorkflowPageQuery()
                .setName(name)
                .setStatus(status)
                .setCreatedBy(UserContextHolder.getCurrentEmployee().getPassport());
        query.setCurrent(current == null ? 1L : current).setSize(size == null ? 10L : size);
        Page<WorkflowBO> page = workflowService.page(query);
        List<WorkflowDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(WorkflowAdapterConvert.INSTANCE::toWorkflowDTO).toList();
        return Response.success(new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal()));
    }

    /** 查询工作流详情 */
    @GetMapping("/{id}")
    public Response<WorkflowDTO> findById(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toWorkflowDTO(workflowService.findById(id)));
    }

    /** 保存工作流 */
    @PostMapping
    public Response<WorkflowDTO> save(@RequestBody @Valid WorkflowCmd cmd) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toWorkflowDTO(
                workflowService.save(cmd, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 更新工作流 */
    @PutMapping("/{id}")
    public Response<WorkflowDTO> update(@PathVariable String id, @RequestBody @Valid WorkflowCmd cmd) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toWorkflowDTO(
                workflowService.update(id, cmd, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 删除工作流 */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        workflowService.delete(id);
        return Response.success();
    }

    /** 查询工作流定义 */
    @GetMapping("/{id}/definition")
    public Response<WorkflowDefinitionDTO> findDefinition(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toDefinitionDTO(workflowService.findDefinition(id)));
    }

    /** 保存工作流定义 */
    @PutMapping("/{id}/definition")
    public Response<WorkflowDefinitionDTO> saveDefinition(@PathVariable String id,
                                                          @RequestBody WorkflowDefinitionCmd cmd) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toDefinitionDTO(
                workflowService.saveDefinition(id, cmd, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 查询工作流级依赖 */
    @GetMapping("/{id}/dependencies")
    public Response<WorkflowDependencyDTO> findDependencies(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toDependencyDTO(workflowService.findDependencies(id)));
    }

    /** 保存工作流级依赖 */
    @PutMapping("/{id}/dependencies")
    public Response<WorkflowDependencyDTO> saveDependencies(@PathVariable String id,
                                                            @RequestBody WorkflowDependencyCmd cmd) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toDependencyDTO(
                workflowService.saveDependencies(id, cmd, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 查询工作流调度配置 */
    @GetMapping("/{id}/schedule")
    public Response<WorkflowScheduleDTO> findSchedule(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toScheduleDTO(workflowService.findSchedule(id)));
    }

    /** 保存工作流调度配置 */
    @PutMapping("/{id}/schedule")
    public Response<WorkflowScheduleDTO> saveSchedule(@PathVariable String id,
                                                      @RequestBody @Valid WorkflowScheduleCmd cmd) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toScheduleDTO(
                workflowService.saveSchedule(id, cmd, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 发布工作流 */
    @PutMapping("/{id}/publish")
    public Response<WorkflowDTO> publish(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toWorkflowDTO(
                workflowService.publish(id, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 下线工作流 */
    @PutMapping("/{id}/offline")
    public Response<WorkflowDTO> offline(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toWorkflowDTO(
                workflowService.offline(id, UserContextHolder.getCurrentEmployee().getPassport())));
    }
}
