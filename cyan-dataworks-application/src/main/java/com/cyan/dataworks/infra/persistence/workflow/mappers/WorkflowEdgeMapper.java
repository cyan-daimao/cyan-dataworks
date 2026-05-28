package com.cyan.dataworks.infra.persistence.workflow.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowEdgeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流依赖边Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface WorkflowEdgeMapper extends BaseMapper<WorkflowEdgeDO> {
}
