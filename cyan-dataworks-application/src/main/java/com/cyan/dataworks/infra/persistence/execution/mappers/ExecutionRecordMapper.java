package com.cyan.dataworks.infra.persistence.execution.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.execution.dos.ExecutionRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行记录Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface ExecutionRecordMapper extends BaseMapper<ExecutionRecordDO> {
}
