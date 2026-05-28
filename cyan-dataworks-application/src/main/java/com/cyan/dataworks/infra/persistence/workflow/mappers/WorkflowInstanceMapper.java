package com.cyan.dataworks.infra.persistence.workflow.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowInstanceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流实例Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceDO> {
}
