package com.cyan.dataworks.application.schedule.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.schedule.bo.ScheduleConfigBO;
import com.cyan.dataworks.application.schedule.cmd.ScheduleConfigCmd;
import com.cyan.dataworks.domain.schedule.ScheduleConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 调度配置应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface ScheduleAppConvert {

    ScheduleAppConvert INSTANCE = Mappers.getMapper(ScheduleAppConvert.class);

    /**
     * Domain 转 BO
     */
    ScheduleConfigBO toScheduleConfigBO(ScheduleConfig scheduleConfig);

    /**
     * Cmd 转 Domain
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "nextExecuteTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ScheduleConfig toScheduleConfig(ScheduleConfigCmd cmd);
}
