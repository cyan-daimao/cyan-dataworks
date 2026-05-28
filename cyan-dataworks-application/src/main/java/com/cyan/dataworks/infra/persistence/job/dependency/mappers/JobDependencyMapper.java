package com.cyan.dataworks.infra.persistence.job.dependency.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.job.dependency.dos.JobDependencyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 作业依赖 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface JobDependencyMapper extends BaseMapper<JobDependencyDO> {

    /**
     * 按下游作业ID软删除依赖，并为每行生成不同删除时间，避免联合唯一索引冲突
     *
     * @param downstreamJobId 下游作业ID
     */
    @Update("""
            UPDATE data_work_job_dependency
            SET deleted_at = TIMESTAMPADD(MICROSECOND, MOD(id, 1000000), NOW(6))
            WHERE deleted_at IS NULL
              AND downstream_job_id = #{downstreamJobId}
            """)
    void softDeleteByDownstreamJobId(@Param("downstreamJobId") Long downstreamJobId);

    /**
     * 按作业ID软删除相关依赖，并为每行生成不同删除时间，避免联合唯一索引冲突
     *
     * @param jobId 作业ID
     */
    @Update("""
            UPDATE data_work_job_dependency
            SET deleted_at = TIMESTAMPADD(MICROSECOND, MOD(id, 1000000), NOW(6))
            WHERE deleted_at IS NULL
              AND (upstream_job_id = #{jobId} OR downstream_job_id = #{jobId})
            """)
    void softDeleteByJobId(@Param("jobId") Long jobId);
}
