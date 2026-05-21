package com.cyan.dataworks.application.job.schedule.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import com.cyan.dataworks.application.job.schedule.cmd.JobScheduleCmd;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 作业调度配置应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobScheduleAppConvert {

    JobScheduleAppConvert INSTANCE = Mappers.getMapper(JobScheduleAppConvert.class);

    /**
     * Domain 转 BO
     */
    JobScheduleBO toJobScheduleBO(JobSchedule jobSchedule);

    /**
     * Cmd 转 Domain
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobId", ignore = true)
    @Mapping(target = "nextExecuteTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobSchedule toJobSchedule(JobScheduleCmd cmd);
}
