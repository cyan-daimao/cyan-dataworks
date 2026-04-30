package com.cyan.dataworks.infra.persistence.schedule.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataworks.domain.schedule.ScheduleConfig;
import com.cyan.dataworks.domain.schedule.repository.ScheduleConfigRepository;
import com.cyan.dataworks.infra.persistence.schedule.convert.ScheduleInfraConvert;
import com.cyan.dataworks.infra.persistence.schedule.dos.ScheduleConfigDO;
import com.cyan.dataworks.infra.persistence.schedule.mappers.ScheduleConfigMapper;
import org.springframework.stereotype.Repository;

/**
 * 调度配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class ScheduleConfigRepositoryImpl implements ScheduleConfigRepository {

    private final ScheduleConfigMapper scheduleConfigMapper;

    public ScheduleConfigRepositoryImpl(ScheduleConfigMapper scheduleConfigMapper) {
        this.scheduleConfigMapper = scheduleConfigMapper;
    }

    /**
     * 根据任务ID查询调度配置
     */
    @Override
    public ScheduleConfig findByTaskId(String taskId) {
        LambdaQueryWrapper<ScheduleConfigDO> wrapper = new LambdaQueryWrapper<ScheduleConfigDO>()
                .eq(ScheduleConfigDO::getTaskId, com.cyan.arch.common.util.Convert.toLong(taskId));
        ScheduleConfigDO scheduleConfigDO = scheduleConfigMapper.selectOne(wrapper);
        if (scheduleConfigDO == null) {
            return null;
        }
        return ScheduleInfraConvert.INSTANCE.toScheduleConfig(scheduleConfigDO);
    }

    /**
     * 保存调度配置
     */
    @Override
    public ScheduleConfig save(ScheduleConfig scheduleConfig) {
        ScheduleConfigDO scheduleConfigDO = ScheduleInfraConvert.INSTANCE.toScheduleConfigDO(scheduleConfig);
        scheduleConfigMapper.insert(scheduleConfigDO);
        return findByTaskId(scheduleConfig.getTaskId());
    }

    /**
     * 更新调度配置
     */
    @Override
    public ScheduleConfig updateById(ScheduleConfig scheduleConfig) {
        ScheduleConfigDO scheduleConfigDO = ScheduleInfraConvert.INSTANCE.toScheduleConfigDO(scheduleConfig);
        scheduleConfigMapper.updateById(scheduleConfigDO);
        return findByTaskId(scheduleConfig.getTaskId());
    }

    /**
     * 根据任务ID删除调度配置
     */
    @Override
    public void deleteByTaskId(String taskId) {
        LambdaQueryWrapper<ScheduleConfigDO> wrapper = new LambdaQueryWrapper<ScheduleConfigDO>()
                .eq(ScheduleConfigDO::getTaskId, com.cyan.arch.common.util.Convert.toLong(taskId));
        scheduleConfigMapper.delete(wrapper);
    }
}
