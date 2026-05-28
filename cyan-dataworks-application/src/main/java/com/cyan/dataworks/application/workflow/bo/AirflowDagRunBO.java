package com.cyan.dataworks.application.workflow.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Airflow DAG Run业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AirflowDagRunBO {

    /** DAG ID */
    private String dagId;

    /** DAG Run ID */
    private String dagRunId;

    /** 状态 */
    private String state;

    /** 逻辑时间 */
    private String logicalDate;

    /** 开始时间 */
    private String startDate;

    /** 结束时间 */
    private String endDate;
}
