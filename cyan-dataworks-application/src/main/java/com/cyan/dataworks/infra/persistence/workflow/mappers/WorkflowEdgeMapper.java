package com.cyan.dataworks.infra.persistence.workflow.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowEdgeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 工作流依赖边Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface WorkflowEdgeMapper extends BaseMapper<WorkflowEdgeDO> {

    /**
     * 按工作流ID软删除依赖边，并为每行生成不同删除时间，避免联合唯一索引冲突
     *
     * @param workflowId 工作流ID
     */
    @Update("""
            UPDATE data_work_workflow_edge
            SET deleted_at = TIMESTAMPADD(MICROSECOND, MOD(id, 1000000), NOW(6))
            WHERE deleted_at IS NULL
              AND workflow_id = #{workflowId}
            """)
    void softDeleteByWorkflowId(@Param("workflowId") Long workflowId);
}
