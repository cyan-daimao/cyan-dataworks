package com.cyan.dataworks.infra.persistence.job_instance.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.job_instance.dos.JobInstanceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据加工作业实例 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface JobInstanceMapper extends BaseMapper<JobInstanceDO> {
}
