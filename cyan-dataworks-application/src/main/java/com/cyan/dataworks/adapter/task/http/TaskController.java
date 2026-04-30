package com.cyan.dataworks.adapter.task.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.task.http.convert.TaskAdapterConvert;
import com.cyan.dataworks.adapter.task.http.dto.DataWorkTaskDTO;
import com.cyan.dataworks.application.task.TaskService;
import com.cyan.dataworks.application.task.bo.DataWorkTaskBO;
import com.cyan.dataworks.application.task.cmd.DataWorkTaskCmd;
import com.cyan.dataworks.domain.task.query.DataWorkTaskPageQuery;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 数据加工任务接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 分页查询任务
     */
    @GetMapping
    public Response<Page<DataWorkTaskDTO>> page(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) EngineType engineType,
                                                 @RequestParam(required = false) Long folderId,
                                                 @RequestParam(required = false) Long current,
                                                 @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        DataWorkTaskPageQuery query = new DataWorkTaskPageQuery()
                .setName(name)
                .setEngineType(engineType)
                .setFolderId(folderId)
                .setCreatedBy(UserContextHolder.getCurrentEmployee().getPassport());
        query.setCurrent(current).setSize(size);
        Page<DataWorkTaskBO> page = taskService.page(query);
        List<DataWorkTaskDTO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(TaskAdapterConvert.INSTANCE::toDataWorkTaskDTO).toList();
        Page<DataWorkTaskDTO> result = new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
        return Response.success(result);
    }

    /**
     * 根据ID查询任务
     */
    @GetMapping("/{id}")
    public Response<DataWorkTaskDTO> findById(@PathVariable String id) {
        DataWorkTaskBO bo = taskService.findById(id);
        DataWorkTaskDTO dto = TaskAdapterConvert.INSTANCE.toDataWorkTaskDTO(bo);
        return Response.success(dto);
    }

    /**
     * 保存任务
     */
    @PostMapping
    public Response<DataWorkTaskDTO> save(@RequestBody @Valid DataWorkTaskCmd cmd) {
        DataWorkTaskBO bo = taskService.save(cmd, UserContextHolder.getCurrentEmployee().getPassport());
        DataWorkTaskDTO dto = TaskAdapterConvert.INSTANCE.toDataWorkTaskDTO(bo);
        return Response.success(dto);
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    public Response<DataWorkTaskDTO> update(@PathVariable String id, @RequestBody @Valid DataWorkTaskCmd cmd) {
        DataWorkTaskBO bo = taskService.update(id, cmd);
        DataWorkTaskDTO dto = TaskAdapterConvert.INSTANCE.toDataWorkTaskDTO(bo);
        return Response.success(dto);
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        taskService.delete(id);
        return Response.success();
    }
}
