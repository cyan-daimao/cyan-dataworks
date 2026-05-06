package com.cyan.dataworks.adapter.job.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.job.http.dto.JobDTO;
import com.cyan.dataworks.application.job.bo.JobBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface JobAdapterConvert {

    JobAdapterConvert INSTANCE = Mappers.getMapper(JobAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    JobDTO toJobDTO(JobBO jobBO);
}
