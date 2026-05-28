package com.cyan.dataworks.adapter.job.dependency.http.dto;

import com.cyan.dataworks.adapter.job.http.dto.JobDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 作业依赖 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDependencyDTO {

    /**
     * 作业ID
     */
    private String jobId;

    /**
     * 上游作业列表
     */
    private List<JobDTO> upstreamJobs;

    /**
     * 下游作业列表
     */
    private List<JobDTO> downstreamJobs;
}
