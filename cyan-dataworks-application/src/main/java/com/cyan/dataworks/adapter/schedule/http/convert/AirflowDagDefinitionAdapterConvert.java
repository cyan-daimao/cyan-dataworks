package com.cyan.dataworks.adapter.schedule.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.schedule.http.dto.AirflowDagDefinitionDTO;
import com.cyan.dataworks.application.schedule.bo.AirflowDagDefinitionBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Airflow DAG定义适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface AirflowDagDefinitionAdapterConvert {

    AirflowDagDefinitionAdapterConvert INSTANCE = Mappers.getMapper(AirflowDagDefinitionAdapterConvert.class);

    /**
     * BO转DTO
     *
     * @param bo DAG定义业务对象
     * @return DAG定义DTO
     */
    AirflowDagDefinitionDTO toDTO(AirflowDagDefinitionBO bo);
}
