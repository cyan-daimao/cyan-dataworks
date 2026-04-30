package com.cyan.dataworks.infra.persistence.schedule.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.schedule.ScheduleConfig;
import com.cyan.dataworks.infra.persistence.schedule.dos.ScheduleConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 调度配置基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface ScheduleInfraConvert {

    ScheduleInfraConvert INSTANCE = Mappers.getMapper(ScheduleInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(scheduleConfigDO.getId()))")
    @Mapping(target = "taskId", expression = "java(com.cyan.arch.common.util.Convert.toStr(scheduleConfigDO.getTaskId()))")
    ScheduleConfig toScheduleConfig(ScheduleConfigDO scheduleConfigDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(scheduleConfig.getId()))")
    @Mapping(target = "taskId", expression = "java(com.cyan.arch.common.util.Convert.toLong(scheduleConfig.getTaskId()))")
    ScheduleConfigDO toScheduleConfigDO(ScheduleConfig scheduleConfig);
}
