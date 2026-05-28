package com.cyan.dataworks.adapter.job.dependency.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.job.dependency.http.dto.JobDependencyDTO;
import com.cyan.dataworks.adapter.job.dependency.http.dto.JobLineageDTO;
import com.cyan.dataworks.application.job.dependency.bo.JobDependencyBO;
import com.cyan.dataworks.application.job.dependency.bo.JobLineageBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 作业依赖适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobDependencyAdapterConvert {

    JobDependencyAdapterConvert INSTANCE = Mappers.getMapper(JobDependencyAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    JobDependencyDTO toJobDependencyDTO(JobDependencyBO bo);

    /**
     * BO 转 DTO
     */
    JobLineageDTO toJobLineageDTO(JobLineageBO bo);
}
