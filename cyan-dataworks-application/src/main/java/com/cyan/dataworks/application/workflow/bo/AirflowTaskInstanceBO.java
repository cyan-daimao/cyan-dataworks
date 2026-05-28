package com.cyan.dataworks.application.workflow.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Airflow任务实例业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AirflowTaskInstanceBO {

    /** DAG ID */
    private String dagId;

    /** DAG Run ID */
    private String dagRunId;

    /** 任务ID */
    private String taskId;

    /** 状态 */
    private String state;

    /** 尝试次数 */
    private Integer tryNumber;

    /** 开始时间 */
    private String startDate;

    /** 结束时间 */
    private String endDate;
}
