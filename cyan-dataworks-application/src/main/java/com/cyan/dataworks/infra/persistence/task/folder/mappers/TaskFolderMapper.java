package com.cyan.dataworks.infra.persistence.task.folder.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.task.folder.dos.TaskFolderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务文件夹Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface TaskFolderMapper extends BaseMapper<TaskFolderDO> {
}
