package com.cyan.dataworks.application.task.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.task.bo.DataWorkTaskBO;
import com.cyan.dataworks.application.task.cmd.DataWorkTaskCmd;
import com.cyan.dataworks.domain.task.DataWorkTask;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工任务应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface TaskAppConvert {

    TaskAppConvert INSTANCE = Mappers.getMapper(TaskAppConvert.class);

    /**
     * Domain 转 BO
     */
    DataWorkTaskBO toDataWorkTaskBO(DataWorkTask dataWorkTask);

    /**
     * Cmd 转 Domain
     */
    DataWorkTask toDataWorkTask(DataWorkTaskCmd cmd);
}
