package com.cyan.dataworks.application.job_instance.cmd;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据加工作业实例命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceCmd {

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * SQL内容
     */
    private String sqlContent;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 耗时（毫秒）
     */
    private Long costTimeMs;

    /**
     * 结果数据（JSON）
     */
    private String resultData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * Flink Application名称
     */
    private String applicationName;

    /**
     * Flink Application命名空间
     */
    private String applicationNamespace;

    /**
     * Flink ConfigMap名称
     */
    private String configMapName;

    /**
     * JobManager Pod名称
     */
    private String jobManagerPodName;

    /**
     * TaskManager Pod名称列表（JSON）
     */
    private String taskManagerPodNames;
}
