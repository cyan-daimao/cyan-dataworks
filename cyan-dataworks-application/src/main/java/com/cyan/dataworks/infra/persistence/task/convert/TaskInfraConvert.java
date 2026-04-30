package com.cyan.dataworks.infra.persistence.task.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.task.DataWorkTask;
import com.cyan.dataworks.infra.persistence.task.dos.DataWorkTaskDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工任务基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface TaskInfraConvert {

    TaskInfraConvert INSTANCE = Mappers.getMapper(TaskInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(dataWorkTaskDO.getId()))")
    DataWorkTask toDataWorkTask(DataWorkTaskDO dataWorkTaskDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(dataWorkTask.getId()))")
    DataWorkTaskDO toDataWorkTaskDO(DataWorkTask dataWorkTask);
}
