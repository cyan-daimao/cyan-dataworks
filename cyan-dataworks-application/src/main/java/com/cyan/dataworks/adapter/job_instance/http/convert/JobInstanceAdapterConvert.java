package com.cyan.dataworks.adapter.job_instance.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.job_instance.http.dto.JobInstanceDTO;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业实例适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface JobInstanceAdapterConvert {

    JobInstanceAdapterConvert INSTANCE = Mappers.getMapper(JobInstanceAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    JobInstanceDTO toJobInstanceDTO(JobInstanceBO jobInstanceBO);
}
