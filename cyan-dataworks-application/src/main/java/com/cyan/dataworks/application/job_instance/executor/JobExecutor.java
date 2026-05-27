package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;

/**
 * 作业执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface JobExecutor {

    /**
     * 是否支持节点类型
     *
     * @param nodeType 节点类型
     * @return 是否支持
     */
    boolean supports(NodeType nodeType);

    /**
     * 执行作业
     *
     * @param job      作业定义
     * @param instance 作业实例
     * @return 执行结果
     */
    JobExecutionResult execute(Job job, JobInstance instance);
}
