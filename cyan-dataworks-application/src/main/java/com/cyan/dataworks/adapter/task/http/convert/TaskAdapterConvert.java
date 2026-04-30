package com.cyan.dataworks.adapter.task.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.task.http.dto.DataWorkTaskDTO;
import com.cyan.dataworks.application.task.bo.DataWorkTaskBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工任务适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface TaskAdapterConvert {

    TaskAdapterConvert INSTANCE = Mappers.getMapper(TaskAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    DataWorkTaskDTO toDataWorkTaskDTO(DataWorkTaskBO dataWorkTaskBO);
}
