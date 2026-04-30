package com.cyan.dataworks.adapter.task.http;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.adapter.task.http.dto.TaskFolderDTO;
import com.cyan.dataworks.domain.task.folder.TaskFolder;
import com.cyan.dataworks.domain.task.folder.TaskFolderRepository;
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

    private final TaskFolderRepository taskFolderRepository;

    public FolderController(TaskFolderRepository taskFolderRepository) {
        this.taskFolderRepository = taskFolderRepository;
    }

    /**
     * 查询文件夹树
     */
    @GetMapping
    public Response<List<TaskFolderDTO>> tree() {
        List<TaskFolder> all = taskFolderRepository.findAll();
        List<TaskFolderDTO> tree = buildTree(all, "0");
        return Response.success(tree);
    }

    /**
     * 创建文件夹
     */
    @PostMapping
    public Response<TaskFolderDTO> save(@RequestBody TaskFolderDTO dto) {
        TaskFolder folder = new TaskFolder()
                .setName(dto.getName())
                .setParentId(dto.getParentId() == null ? "0" : dto.getParentId());
        folder = folder.save(taskFolderRepository);
        return Response.success(toDTO(folder));
    }

    /**
     * 更新文件夹
     */
    @PutMapping("/{id}")
    public Response<TaskFolderDTO> update(@PathVariable String id, @RequestBody TaskFolderDTO dto) {
        TaskFolder existing = taskFolderRepository.findById(id);
        Assert.notNull(existing, new SilentException("文件夹不存在"));
        TaskFolder folder = new TaskFolder()
                .setId(id)
                .setName(dto.getName())
                .setParentId(dto.getParentId());
        folder = folder.update(taskFolderRepository);
        return Response.success(toDTO(folder));
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        TaskFolder existing = taskFolderRepository.findById(id);
        Assert.notNull(existing, new SilentException("文件夹不存在"));
        existing.delete(taskFolderRepository);
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
