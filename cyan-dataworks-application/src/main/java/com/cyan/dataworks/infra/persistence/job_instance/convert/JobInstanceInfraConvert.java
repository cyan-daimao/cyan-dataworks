package com.cyan.dataworks.infra.persistence.job_instance.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.infra.persistence.job_instance.dos.JobInstanceDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业实例基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobInstanceInfraConvert {

    JobInstanceInfraConvert INSTANCE = Mappers.getMapper(JobInstanceInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobInstanceDO.getId()))")
    @Mapping(target = "jobId", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobInstanceDO.getJobId()))")
    JobInstance toJobInstance(JobInstanceDO jobInstanceDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobInstance.getId()))")
    @Mapping(target = "jobId", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobInstance.getJobId()))")
    JobInstanceDO toJobInstanceDO(JobInstance jobInstance);
}
