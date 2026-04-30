package com.cyan.dataworks.adapter.execution.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.execution.http.convert.ExecutionAdapterConvert;
import com.cyan.dataworks.adapter.execution.http.dto.ExecutionRecordDTO;
import com.cyan.dataworks.application.execution.ExecutionService;
import com.cyan.dataworks.application.execution.bo.ExecutionRecordBO;
import com.cyan.dataworks.domain.execution.query.ExecutionRecordPageQuery;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 执行记录接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * 手动执行任务
     */
    @PostMapping("/tasks/{id}/execute")
    public Response<ExecutionRecordDTO> execute(@PathVariable String id) {
        ExecutionRecordBO bo = executionService.execute(id);
        ExecutionRecordDTO dto = ExecutionAdapterConvert.INSTANCE.toExecutionRecordDTO(bo);
        return Response.success(dto);
    }

    /**
     * 分页查询执行记录
     */
    @GetMapping("/tasks/{taskId}/executions")
    public Response<Page<ExecutionRecordDTO>> page(@PathVariable String taskId,
                                                    @RequestParam(required = false) Long current,
                                                    @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        ExecutionRecordPageQuery query = new ExecutionRecordPageQuery()
                .setTaskId(taskId)
                .setCurrent(current)
                .setSize(size);
        Page<ExecutionRecordBO> page = executionService.page(query);
        List<ExecutionRecordDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(ExecutionAdapterConvert.INSTANCE::toExecutionRecordDTO).toList();
        Page<ExecutionRecordDTO> result = new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
        return Response.success(result);
    }

    /**
     * 根据ID查询执行记录
     */
    @GetMapping("/executions/{id}")
    public Response<ExecutionRecordDTO> findById(@PathVariable String id) {
        ExecutionRecordBO bo = executionService.findById(id);
        ExecutionRecordDTO dto = ExecutionAdapterConvert.INSTANCE.toExecutionRecordDTO(bo);
        return Response.success(dto);
    }
}
