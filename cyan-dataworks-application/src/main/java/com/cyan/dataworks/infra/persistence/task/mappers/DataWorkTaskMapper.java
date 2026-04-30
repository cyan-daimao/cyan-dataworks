package com.cyan.dataworks.infra.persistence.task.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.task.dos.DataWorkTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据加工任务Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface DataWorkTaskMapper extends BaseMapper<DataWorkTaskDO> {
}
