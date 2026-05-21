package com.cyan.dataworks.infra.persistence.job.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.infra.persistence.job.dos.JobDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobInfraConvert {

    JobInfraConvert INSTANCE = Mappers.getMapper(JobInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobDO.getId()))")
    Job toJob(JobDO jobDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(job.getId()))")
    JobDO toJobDO(Job job);
}
