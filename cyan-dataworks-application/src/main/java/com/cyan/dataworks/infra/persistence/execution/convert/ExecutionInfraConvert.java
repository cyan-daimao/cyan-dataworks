package com.cyan.dataworks.infra.persistence.execution.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.domain.execution.ExecutionRecord;
import com.cyan.dataworks.infra.persistence.execution.dos.ExecutionRecordDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 执行记录基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface ExecutionInfraConvert {

    ExecutionInfraConvert INSTANCE = Mappers.getMapper(ExecutionInfraConvert.class);

    /**
     * DO 转 Domain
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toStr(executionRecordDO.getId()))")
    @Mapping(target = "taskId", expression = "java(com.cyan.arch.common.util.Convert.toStr(executionRecordDO.getTaskId()))")
    ExecutionRecord toExecutionRecord(ExecutionRecordDO executionRecordDO);

    /**
     * Domain 转 DO
     */
    @Mapping(target = "id", expression = "java(com.cyan.arch.common.util.Convert.toLong(executionRecord.getId()))")
    @Mapping(target = "taskId", expression = "java(com.cyan.arch.common.util.Convert.toLong(executionRecord.getTaskId()))")
    ExecutionRecordDO toExecutionRecordDO(ExecutionRecord executionRecord);
}
