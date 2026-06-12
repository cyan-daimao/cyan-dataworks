package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;
import org.springframework.stereotype.Component;

/**
 * 批任务占位执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class UnsupportedBatchJobExecutor implements JobExecutor {

    /**
     * 是否支持节点类型
     */
    @Override
    public boolean supports(NodeType nodeType) {
        return nodeType == NodeType.SPARK_BATCH;
    }

    /**
     * 执行批任务
     */
    @Override
    public JobExecutionResult execute(Job job, JobInstance instance) {
        throw new SilentException("批任务提交执行器尚未配置，请在configJson中补充SparkApplication/FlinkDeployment提交实现");
    }
}
