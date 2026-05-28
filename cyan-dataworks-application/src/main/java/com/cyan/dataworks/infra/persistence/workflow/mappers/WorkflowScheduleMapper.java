package com.cyan.dataworks.infra.persistence.workflow.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowScheduleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 工作流调度配置Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface WorkflowScheduleMapper extends BaseMapper<WorkflowScheduleDO> {

    /**
     * 按工作流ID软删除调度配置，并为每行生成不同删除时间，避免联合唯一索引冲突
     *
     * @param workflowId 工作流ID
     */
    @Update("""
            UPDATE data_work_workflow_schedule
            SET deleted_at = TIMESTAMPADD(MICROSECOND, MOD(id, 1000000), NOW(6))
            WHERE deleted_at IS NULL
              AND workflow_id = #{workflowId}
            """)
    void softDeleteByWorkflowId(@Param("workflowId") Long workflowId);
}
