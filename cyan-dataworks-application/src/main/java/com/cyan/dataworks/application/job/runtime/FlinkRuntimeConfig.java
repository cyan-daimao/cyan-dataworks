package com.cyan.dataworks.application.job.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Flink运行配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlinkRuntimeConfig {

    /**
     * TaskManager内存，单位GB
     */
    private Integer taskManagerMemoryGb;

    /**
     * TaskManager CPU核数
     */
    private Double taskManagerCpu;

    /**
     * Flink作业并行度
     */
    private Integer parallelism;
}
