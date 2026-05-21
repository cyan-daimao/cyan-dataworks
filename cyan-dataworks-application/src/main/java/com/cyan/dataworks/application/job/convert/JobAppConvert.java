package com.cyan.dataworks.application.job.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.cmd.JobCmd;
import com.cyan.dataworks.domain.job.Job;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobAppConvert {

    JobAppConvert INSTANCE = Mappers.getMapper(JobAppConvert.class);

    /**
     * Domain 转 BO
     */
    JobBO toJobBO(Job job);

    /**
     * Cmd 转 Domain
     */
    Job toJob(JobCmd cmd);
}
