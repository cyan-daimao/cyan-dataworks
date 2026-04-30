package com.cyan.dataworks.adapter.schedule.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.schedule.http.dto.ScheduleConfigDTO;
import com.cyan.dataworks.application.schedule.bo.ScheduleConfigBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 调度配置适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface ScheduleAdapterConvert {

    ScheduleAdapterConvert INSTANCE = Mappers.getMapper(ScheduleAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    ScheduleConfigDTO toScheduleConfigDTO(ScheduleConfigBO scheduleConfigBO);
}
