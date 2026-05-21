package com.cyan.dataworks.infra.persistence.task.folder.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.task.folder.TaskFolder;
import com.cyan.dataworks.infra.persistence.task.folder.dos.TaskFolderDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 任务文件夹基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface TaskFolderInfraConvert {

    TaskFolderInfraConvert INSTANCE = Mappers.getMapper(TaskFolderInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(dataWorkTaskFolderDO.getId() != null ? dataWorkTaskFolderDO.getId().toString() : null)")
    @Mapping(target = "parentId", expression = "java(dataWorkTaskFolderDO.getParentId() != null ? dataWorkTaskFolderDO.getParentId().toString() : \"0\")")
    TaskFolder toTaskFolder(TaskFolderDO dataWorkTaskFolderDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(taskFolder.getId() != null ? Long.valueOf(taskFolder.getId()) : null)")
    @Mapping(target = "parentId", expression = "java(taskFolder.getParentId() != null ? Long.valueOf(taskFolder.getParentId()) : 0L)")
    TaskFolderDO toTaskFolderDO(TaskFolder taskFolder);
}
