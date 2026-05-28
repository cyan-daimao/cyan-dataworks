package com.cyan.dataworks.infra.persistence.job.dependency.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.job.dependency.dos.JobDependencyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业依赖 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface JobDependencyMapper extends BaseMapper<JobDependencyDO> {
}
