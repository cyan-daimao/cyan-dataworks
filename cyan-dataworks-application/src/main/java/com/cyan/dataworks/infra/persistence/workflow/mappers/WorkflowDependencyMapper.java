package com.cyan.dataworks.infra.persistence.workflow.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowDependencyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流级依赖Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface WorkflowDependencyMapper extends BaseMapper<WorkflowDependencyDO> {
}
