package com.cyan.dataworks.infra.persistence.workflow.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowDO> {
}
