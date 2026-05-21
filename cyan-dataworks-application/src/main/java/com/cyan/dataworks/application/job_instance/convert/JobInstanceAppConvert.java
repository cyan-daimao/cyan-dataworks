package com.cyan.dataworks.application.job_instance.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.cmd.JobInstanceCmd;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业实例应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobInstanceAppConvert {

    JobInstanceAppConvert INSTANCE = Mappers.getMapper(JobInstanceAppConvert.class);

    /**
     * Domain 转 BO
     */
    JobInstanceBO toJobInstanceBO(JobInstance jobInstance);

    /**
     * Cmd 转 Domain
     */
    JobInstance toJobInstance(JobInstanceCmd cmd);
}
