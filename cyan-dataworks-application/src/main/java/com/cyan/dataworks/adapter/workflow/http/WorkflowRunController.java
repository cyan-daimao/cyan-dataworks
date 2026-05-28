package com.cyan.dataworks.adapter.workflow.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.workflow.http.convert.WorkflowAdapterConvert;
import com.cyan.dataworks.adapter.workflow.http.dto.AirflowDagRunDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.AirflowTaskInstanceDTO;
import com.cyan.dataworks.adapter.workflow.http.dto.DagRunStateUpdateRequest;
import com.cyan.dataworks.adapter.workflow.http.dto.TaskInstanceStateUpdateRequest;
import com.cyan.dataworks.adapter.workflow.http.dto.WorkflowInstanceDTO;
import com.cyan.dataworks.application.workflow.WorkflowRunService;
import com.cyan.dataworks.application.workflow.bo.WorkflowInstanceBO;
import com.cyan.dataworks.domain.workflow.query.WorkflowInstancePageQuery;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 工作流运行接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/workflows")
public class WorkflowRunController {

    /** 工作流运行应用服务 */
    private final WorkflowRunService workflowRunService;

    public WorkflowRunController(WorkflowRunService workflowRunService) {
        this.workflowRunService = workflowRunService;
    }

    /** 触发DAG Run */
    @PostMapping("/{id}/dag-runs")
    public Response<WorkflowInstanceDTO> trigger(@PathVariable String id) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toInstanceDTO(
                workflowRunService.trigger(id, UserContextHolder.getCurrentEmployee().getPassport())));
    }

    /** 查询本地工作流实例 */
    @GetMapping("/{id}/instances")
    public Response<Page<WorkflowInstanceDTO>> pageInstances(@PathVariable String id,
                                                             @RequestParam(required = false) ExecutionStatus status,
                                                             @RequestParam(required = false) Long current,
                                                             @RequestParam(required = false) Long size) {
        WorkflowInstancePageQuery query = new WorkflowInstancePageQuery()
                .setWorkflowId(id)
                .setStatus(status);
        query.setCurrent(current == null ? 1L : current).setSize(size == null ? 10L : size);
        Page<WorkflowInstanceBO> page = workflowRunService.pageInstances(query);
        List<WorkflowInstanceDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(WorkflowAdapterConvert.INSTANCE::toInstanceDTO).toList();
        return Response.success(new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal()));
    }

    /** 查询DAG Run历史 */
    @GetMapping("/{id}/dag-runs")
    public Response<List<AirflowDagRunDTO>> listDagRuns(@PathVariable String id,
                                                        @RequestParam(required = false) Integer limit,
                                                        @RequestParam(required = false) Integer offset) {
        return Response.success(workflowRunService.listDagRuns(id, limit, offset).stream()
                .map(WorkflowAdapterConvert.INSTANCE::toDagRunDTO).toList());
    }

    /** 查询DAG Run详情 */
    @GetMapping("/{id}/dag-runs/{runId}")
    public Response<AirflowDagRunDTO> getDagRun(@PathVariable String id, @PathVariable String runId) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toDagRunDTO(workflowRunService.getDagRun(id, runId)));
    }

    /** 更新DAG Run状态 */
    @PatchMapping("/{id}/dag-runs/{runId}/state")
    public Response<AirflowDagRunDTO> updateDagRunState(@PathVariable String id,
                                                        @PathVariable String runId,
                                                        @RequestBody @Valid DagRunStateUpdateRequest request) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toDagRunDTO(
                workflowRunService.updateDagRunState(id, runId, request.getState())));
    }

    /** 查询任务实例列表 */
    @GetMapping("/{id}/dag-runs/{runId}/task-instances")
    public Response<List<AirflowTaskInstanceDTO>> listTaskInstances(@PathVariable String id,
                                                                    @PathVariable String runId) {
        return Response.success(workflowRunService.listTaskInstances(id, runId).stream()
                .map(WorkflowAdapterConvert.INSTANCE::toTaskInstanceDTO).toList());
    }

    /** 重跑任务实例 */
    @PostMapping("/{id}/dag-runs/{runId}/task-instances/{taskId}/rerun")
    public Response<Void> rerunTaskInstance(@PathVariable String id,
                                            @PathVariable String runId,
                                            @PathVariable String taskId) {
        workflowRunService.rerunTaskInstance(id, runId, taskId);
        return Response.success();
    }

    /** 更新任务实例状态 */
    @PatchMapping("/{id}/dag-runs/{runId}/task-instances/{taskId}/state")
    public Response<AirflowTaskInstanceDTO> updateTaskInstanceState(@PathVariable String id,
                                                                    @PathVariable String runId,
                                                                    @PathVariable String taskId,
                                                                    @RequestBody @Valid TaskInstanceStateUpdateRequest request) {
        return Response.success(WorkflowAdapterConvert.INSTANCE.toTaskInstanceDTO(
                workflowRunService.updateTaskInstanceState(id, runId, taskId, request.getState())));
    }
}
