package com.cyan.dataworks.application.job.dependency.bo;

import com.cyan.dataworks.application.job.bo.JobBO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 作业依赖业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDependencyBO {

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 上游作业列表
     */
    private List<JobBO> upstreamJobs;

    /**
     * 下游作业列表
     */
    private List<JobBO> downstreamJobs;
}
