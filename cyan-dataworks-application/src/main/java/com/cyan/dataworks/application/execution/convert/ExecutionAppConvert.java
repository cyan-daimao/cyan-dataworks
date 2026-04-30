package com.cyan.dataworks.application.execution.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.application.execution.bo.ExecutionRecordBO;
import com.cyan.dataworks.application.execution.cmd.ExecutionRecordCmd;
import com.cyan.dataworks.domain.execution.ExecutionRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 执行记录应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface ExecutionAppConvert {

    ExecutionAppConvert INSTANCE = Mappers.getMapper(ExecutionAppConvert.class);

    /**
     * Domain 转 BO
     */
    ExecutionRecordBO toExecutionRecordBO(ExecutionRecord executionRecord);

    /**
     * Cmd 转 Domain
     */
    ExecutionRecord toExecutionRecord(ExecutionRecordCmd cmd);
}
