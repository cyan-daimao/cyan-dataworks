package com.cyan.dataworks.infra.persistence.schedule.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.schedule.dos.ScheduleConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调度配置Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface ScheduleConfigMapper extends BaseMapper<ScheduleConfigDO> {
}
