package com.cyan.dataworks.adapter.execution.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.execution.http.dto.ExecutionRecordDTO;
import com.cyan.dataworks.application.execution.bo.ExecutionRecordBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 执行记录适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface ExecutionAdapterConvert {

    ExecutionAdapterConvert INSTANCE = Mappers.getMapper(ExecutionAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    ExecutionRecordDTO toExecutionRecordDTO(ExecutionRecordBO executionRecordBO);
}
