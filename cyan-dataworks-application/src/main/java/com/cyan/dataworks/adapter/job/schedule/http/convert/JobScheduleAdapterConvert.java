package com.cyan.dataworks.adapter.job.schedule.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.job.schedule.http.dto.JobScheduleDTO;
import com.cyan.dataworks.application.job.schedule.bo.JobScheduleBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 作业调度配置适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobScheduleAdapterConvert {

    JobScheduleAdapterConvert INSTANCE = Mappers.getMapper(JobScheduleAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    JobScheduleDTO toJobScheduleDTO(JobScheduleBO jobScheduleBO);
}
