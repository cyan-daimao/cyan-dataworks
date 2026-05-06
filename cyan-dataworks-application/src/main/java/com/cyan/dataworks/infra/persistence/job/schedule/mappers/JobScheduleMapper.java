package com.cyan.dataworks.infra.persistence.job.schedule.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.job.schedule.dos.JobScheduleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业调度配置Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface JobScheduleMapper extends BaseMapper<JobScheduleDO> {
}
