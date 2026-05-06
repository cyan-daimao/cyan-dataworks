package com.cyan.dataworks.domain.job_instance.query;

import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 作业实例分页查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstancePageQuery {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;
}
