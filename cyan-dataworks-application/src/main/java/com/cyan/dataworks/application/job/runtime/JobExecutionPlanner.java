package com.cyan.dataworks.application.job.runtime;

import com.cyan.dataworks.domain.job.Job;
import org.springframework.stereotype.Component;

/**
 * 作业执行计划生成器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class JobExecutionPlanner {

    /**
     * 生成可提交到执行引擎的SQL
     */
    public String buildExecutableSql(Job job) {
        job.validateDefinition();
        return job.getSqlContent();
    }
}
