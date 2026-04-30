package com.cyan.dataworks.infra.persistence.execution.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.domain.execution.ExecutionRecord;
import com.cyan.dataworks.domain.execution.query.ExecutionRecordPageQuery;
import com.cyan.dataworks.domain.execution.repository.ExecutionRecordRepository;
import com.cyan.dataworks.infra.persistence.execution.convert.ExecutionInfraConvert;
import com.cyan.dataworks.infra.persistence.execution.dos.ExecutionRecordDO;
import com.cyan.dataworks.infra.persistence.execution.mappers.ExecutionRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 执行记录仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class ExecutionRecordRepositoryImpl implements ExecutionRecordRepository {

    private final ExecutionRecordMapper executionRecordMapper;

    public ExecutionRecordRepositoryImpl(ExecutionRecordMapper executionRecordMapper) {
        this.executionRecordMapper = executionRecordMapper;
    }

    /**
     * 分页查询执行记录
     */
    @Override
    public Page<ExecutionRecord> page(ExecutionRecordPageQuery query) {
        LambdaQueryWrapper<ExecutionRecordDO> wrapper = new LambdaQueryWrapper<ExecutionRecordDO>()
                .eq(query.getTaskId() != null, ExecutionRecordDO::getTaskId, com.cyan.arch.common.util.Convert.toLong(query.getTaskId()))
                .orderByDesc(ExecutionRecordDO::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExecutionRecordDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = executionRecordMapper.selectPage(page, wrapper);
        List<ExecutionRecord> data = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(ExecutionInfraConvert.INSTANCE::toExecutionRecord).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据ID查询执行记录
     */
    @Override
    public ExecutionRecord findById(String id) {
        ExecutionRecordDO executionRecordDO = executionRecordMapper.selectById(id);
        if (executionRecordDO == null) {
            return null;
        }
        return ExecutionInfraConvert.INSTANCE.toExecutionRecord(executionRecordDO);
    }

    /**
     * 保存执行记录
     */
    @Override
    public ExecutionRecord save(ExecutionRecord executionRecord) {
        ExecutionRecordDO executionRecordDO = ExecutionInfraConvert.INSTANCE.toExecutionRecordDO(executionRecord);
        executionRecordMapper.insert(executionRecordDO);
        return findById(executionRecordDO.getId() + "");
    }

    /**
     * 更新执行记录
     */
    @Override
    public ExecutionRecord updateById(ExecutionRecord executionRecord) {
        ExecutionRecordDO executionRecordDO = ExecutionInfraConvert.INSTANCE.toExecutionRecordDO(executionRecord);
        executionRecordMapper.updateById(executionRecordDO);
        return findById(executionRecord.getId());
    }
}
