package com.cyan.dataworks.infra.persistence.job.dependency.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.job.dependency.JobDependency;
import com.cyan.dataworks.infra.persistence.job.dependency.dos.JobDependencyDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 作业依赖基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobDependencyInfraConvert {

    JobDependencyInfraConvert INSTANCE = Mappers.getMapper(JobDependencyInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobDependencyDO.getId()))")
    @Mapping(target = "upstreamJobId", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobDependencyDO.getUpstreamJobId()))")
    @Mapping(target = "downstreamJobId", expression = "java(com.cyan.arch.common.util.Convert.toStr(jobDependencyDO.getDownstreamJobId()))")
    JobDependency toJobDependency(JobDependencyDO jobDependencyDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobDependency.getId()))")
    @Mapping(target = "upstreamJobId", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobDependency.getUpstreamJobId()))")
    @Mapping(target = "downstreamJobId", expression = "java(com.cyan.arch.common.util.Convert.toLong(jobDependency.getDownstreamJobId()))")
    JobDependencyDO toJobDependencyDO(JobDependency jobDependency);
}
