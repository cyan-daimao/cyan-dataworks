package com.cyan.dataworks.application.workflow.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Airflow DAG业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AirflowDagBO {

    /** DAG ID */
    private String dagId;

    /** 是否暂停 */
    private Boolean paused;

    /** 是否活跃 */
    private Boolean active;
}
