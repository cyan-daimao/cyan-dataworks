package com.cyan.dataworks.application.job_instance.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 作业执行结果
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobExecutionResult {

    /**
     * 结果数据
     */
    private String resultData;

    /**
     * 是否已异步提交运行
     */
    private Boolean asyncSubmitted;

    /**
     * 运行时任务名称
     */
    private String runtimeJobName;
}
