package com.cyan.dataworks.application.schedule;

import com.cyan.dataworks.application.schedule.bo.AirflowDagDefinitionBO;

import java.util.List;

/**
 * Airflow DAG定义应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface AirflowDagDefinitionService {

    /**
     * 查询启用的DAG定义
     *
     * @return DAG定义列表
     */
    List<AirflowDagDefinitionBO> listEnabledDefinitions();
}
