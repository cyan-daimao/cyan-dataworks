package com.cyan.dataworks.infra.persistence.job.schedule.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.job.schedule.JobSchedule;
import com.cyan.dataworks.infra.persistence.job.schedule.dos.JobScheduleDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 作业调度配置基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobScheduleInfraConvert {

    JobScheduleInfraConvert INSTANCE = Mappers.getMapper(JobScheduleInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobScheduleDO.getId()))")
    @Mapping(target = "jobId", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobScheduleDO.getJobId()))")
    JobSchedule toJobSchedule(JobScheduleDO jobScheduleDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobSchedule.getId()))")
    @Mapping(target = "jobId", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobSchedule.getJobId()))")
    JobScheduleDO toJobScheduleDO(JobSchedule jobSchedule);
}
