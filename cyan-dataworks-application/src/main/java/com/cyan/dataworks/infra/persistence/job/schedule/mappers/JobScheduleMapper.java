package com.cyan.dataworks.infra.persistence.job.schedule.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.job.schedule.dos.JobScheduleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 作业调度配置Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface JobScheduleMapper extends BaseMapper<JobScheduleDO> {

    /**
     * 按作业ID软删除调度配置，并为每行生成不同删除时间，避免联合唯一索引冲突
     *
     * @param jobId 作业ID
     */
    @Update("""
            UPDATE data_work_job_schedule
            SET deleted_at = TIMESTAMPADD(MICROSECOND, MOD(id, 1000000), NOW(6))
            WHERE deleted_at IS NULL
              AND job_id = #{jobId}
            """)
    void softDeleteByJobId(@Param("jobId") Long jobId);
}
