package com.cyan.dataworks.application.job.dependency.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.job.dependency.bo.JobLineageBO;
import com.cyan.dataworks.domain.job.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 作业依赖应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobDependencyAppConvert {

    JobDependencyAppConvert INSTANCE = Mappers.getMapper(JobDependencyAppConvert.class);

    /**
     * Job 转血缘节点
     */
    @Mapping(target = "jobId", source = "id")
    @Mapping(target = "jobName", source = "name")
    JobLineageBO.NodeBO toNodeBO(Job job);
}
