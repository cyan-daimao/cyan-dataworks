package com.cyan.dataworks.client.task_folder;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.task_folder.dto.TaskFolderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务文件夹 Feign 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "taskFolderClient", path = "/api/v1/data-work/folders", url = "${feign.cyan-dataworks.url:}")
public interface TaskFolderClient {

    /**
     * 查询文件夹树
     */
    @GetMapping
    Response<List<TaskFolderDTO>> tree();

    /**
     * 创建文件夹
     */
    @PostMapping
    Response<TaskFolderDTO> save(@RequestBody TaskFolderDTO dto);

    /**
     * 更新文件夹
     */
    @PutMapping("/{id}")
    Response<TaskFolderDTO> update(@PathVariable String id, @RequestBody TaskFolderDTO dto);

    /**
     * 删除文件夹
     */
    @DeleteMapping("/{id}")
    Response<Void> delete(@PathVariable String id);
}
