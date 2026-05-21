package com.cyan.dataworks.adapter.task.http;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.adapter.task.http.dto.TaskFolderDTO;
import com.cyan.dataworks.application.task.TaskFolderService;
import com.cyan.dataworks.domain.task.folder.TaskFolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务文件夹接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/folders")
public class FolderController {

    private final TaskFolderService taskFolderService;

    public FolderController(TaskFolderService taskFolderService) {
        this.taskFolderService = taskFolderService;
    }

    /**
     * 查询文件夹树
     */
    @GetMapping
    public Response<List<TaskFolderDTO>> tree() {
        List<TaskFolder> all = taskFolderService.findAll();
        List<TaskFolderDTO> tree = buildTree(all, "0");
        return Response.success(tree);
    }

    /**
     * 创建文件夹
     */
    @PostMapping
    public Response<TaskFolderDTO> save(@RequestBody TaskFolderDTO dto) {
        TaskFolder folder = taskFolderService.save(dto.getName(), dto.getParentId());
        return Response.success(toDTO(folder));
    }

    /**
     * 更新文件夹
     */
    @PutMapping("/{id}")
    public Response<TaskFolderDTO> update(@PathVariable String id, @RequestBody TaskFolderDTO dto) {
        TaskFolder folder = taskFolderService.update(id, dto.getName(), dto.getParentId());
        return Response.success(toDTO(folder));
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        taskFolderService.delete(id);
        return Response.success();
    }

    private List<TaskFolderDTO> buildTree(List<TaskFolder> all, String parentId) {
        return all.stream()
                .filter(f -> parentId.equals(f.getParentId()))
                .map(this::toDTO)
                .peek(dto -> dto.setChildren(buildTree(all, dto.getId())))
                .toList();
    }

    private TaskFolderDTO toDTO(TaskFolder folder) {
        return new TaskFolderDTO()
                .setId(folder.getId())
                .setName(folder.getName())
                .setParentId(folder.getParentId())
                .setCreatedBy(folder.getCreatedBy())
                .setCreatedAt(folder.getCreatedAt())
                .setUpdatedAt(folder.getUpdatedAt());
    }
}
