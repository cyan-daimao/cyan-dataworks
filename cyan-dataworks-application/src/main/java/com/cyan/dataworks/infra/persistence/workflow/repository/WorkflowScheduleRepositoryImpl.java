package com.cyan.dataworks.infra.persistence.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataworks.domain.workflow.WorkflowSchedule;
import com.cyan.dataworks.domain.workflow.repository.WorkflowScheduleRepository;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.infra.persistence.workflow.convert.WorkflowInfraConvert;
import com.cyan.dataworks.infra.persistence.workflow.dos.WorkflowScheduleDO;
import com.cyan.dataworks.infra.persistence.workflow.mappers.WorkflowScheduleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流调度配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class WorkflowScheduleRepositoryImpl implements WorkflowScheduleRepository {

    /** 工作流调度配置Mapper */
    private final WorkflowScheduleMapper workflowScheduleMapper;

    public WorkflowScheduleRepositoryImpl(WorkflowScheduleMapper workflowScheduleMapper) {
        this.workflowScheduleMapper = workflowScheduleMapper;
    }

    /**
     * 根据工作流ID查询调度配置
     */
    @Override
    public WorkflowSchedule findByWorkflowId(String workflowId) {
        WorkflowScheduleDO scheduleDO = workflowScheduleMapper.selectOne(new LambdaQueryWrapper<WorkflowScheduleDO>()
                .eq(WorkflowScheduleDO::getWorkflowId, Convert.toLong(workflowId))
                .last("LIMIT 1"));
        return scheduleDO == null ? null : WorkflowInfraConvert.INSTANCE.toSchedule(scheduleDO);
    }

    /**
     * 查询Airflow调度配置
     */
    @Override
    public List<WorkflowSchedule> listAirflow() {
        return Optional.ofNullable(workflowScheduleMapper.selectList(new LambdaQueryWrapper<WorkflowScheduleDO>()
                        .eq(WorkflowScheduleDO::getSchedulerType, SchedulerType.AIRFLOW)))
                .orElse(List.of())
                .stream().map(WorkflowInfraConvert.INSTANCE::toSchedule).toList();
    }

    /**
     * 保存调度配置
     */
    @Override
    public WorkflowSchedule save(WorkflowSchedule schedule) {
        WorkflowScheduleDO scheduleDO = WorkflowInfraConvert.INSTANCE.toScheduleDO(schedule);
        workflowScheduleMapper.insert(scheduleDO);
        return findByWorkflowId(schedule.getWorkflowId());
    }

    /**
     * 更新调度配置
     */
    @Override
    public WorkflowSchedule updateById(WorkflowSchedule schedule) {
        WorkflowScheduleDO scheduleDO = WorkflowInfraConvert.INSTANCE.toScheduleDO(schedule);
        workflowScheduleMapper.updateById(scheduleDO);
        return findByWorkflowId(schedule.getWorkflowId());
    }

    /**
     * 根据工作流ID删除调度配置
     */
    @Override
    public void deleteByWorkflowId(String workflowId) {
        workflowScheduleMapper.softDeleteByWorkflowId(Convert.toLong(workflowId));
    }
}
