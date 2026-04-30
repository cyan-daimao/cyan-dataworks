package com.cyan.dataworks.domain.task.folder;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 任务文件夹领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TaskFolder {

    /**
     * 主键
     */
    private String id;

    /**
     * 文件夹名称
     */
    private String name;

    /**
     * 父文件夹ID（0表示根目录）
     */
    private String parentId;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 保存文件夹
     */
    public TaskFolder save(TaskFolderRepository repository) {
        Assert.isBlank(this.id, new SilentException("新增时id必须为空"));
        Assert.notBlank(this.name, new SilentException("文件夹名称不能为空"));
        if (this.parentId == null) {
            this.parentId = "0";
        }
        return repository.save(this);
    }

    /**
     * 更新文件夹
     */
    public TaskFolder update(TaskFolderRepository repository) {
        Assert.notBlank(this.id, new SilentException("更新时id不能为空"));
        Assert.notBlank(this.name, new SilentException("文件夹名称不能为空"));
        return repository.updateById(this);
    }

    /**
     * 删除文件夹
     */
    public void delete(TaskFolderRepository repository) {
        Assert.notBlank(this.id, new SilentException("删除时id不能为空"));
        repository.deleteById(this.id);
    }
}
