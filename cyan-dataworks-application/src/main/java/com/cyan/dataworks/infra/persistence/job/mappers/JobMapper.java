package com.cyan.dataworks.infra.persistence.job.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.job.dos.JobDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据加工作业 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface JobMapper extends BaseMapper<JobDO> {
}
