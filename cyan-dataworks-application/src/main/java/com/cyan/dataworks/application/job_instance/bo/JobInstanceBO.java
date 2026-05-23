package com.cyan.dataworks.application.job_instance.bo;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业实例业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceBO {

    /**
     * 主键
     */
    private String id;

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

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;
}
