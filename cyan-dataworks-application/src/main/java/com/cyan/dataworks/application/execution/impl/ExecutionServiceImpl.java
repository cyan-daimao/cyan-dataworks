package com.cyan.dataworks.application.execution.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.JSON;
import com.cyan.dataworks.application.execution.ExecutionService;
import com.cyan.dataworks.application.execution.bo.ExecutionRecordBO;
import com.cyan.dataworks.application.execution.cmd.ExecutionRecordCmd;
import com.cyan.dataworks.application.execution.convert.ExecutionAppConvert;
import com.cyan.dataworks.domain.execution.ExecutionRecord;
import com.cyan.dataworks.domain.execution.query.ExecutionRecordPageQuery;
import com.cyan.dataworks.domain.execution.repository.ExecutionRecordRepository;
import com.cyan.dataworks.domain.task.DataWorkTask;
import com.cyan.dataworks.domain.task.repository.DataWorkTaskRepository;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.ExecutionStatus;
import com.cyan.dataworks.infra.rpc.FlinkRpcClient;
import com.cyan.datagateway.client.SqlGatewayClient;
import com.cyan.datagateway.client.cmd.SqlExecuteCmd;
import com.cyan.datagateway.client.dto.SqlExecuteResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 执行记录应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class ExecutionServiceImpl implements ExecutionService {

    private final DataWorkTaskRepository dataWorkTaskRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final SqlGatewayClient sqlGatewayClient;
    private final FlinkRpcClient flinkRpcClient;

    public ExecutionServiceImpl(DataWorkTaskRepository dataWorkTaskRepository,
                                ExecutionRecordRepository executionRecordRepository,
                                SqlGatewayClient sqlGatewayClient,
                                FlinkRpcClient flinkRpcClient) {
        this.dataWorkTaskRepository = dataWorkTaskRepository;
        this.executionRecordRepository = executionRecordRepository;
        this.sqlGatewayClient = sqlGatewayClient;
        this.flinkRpcClient = flinkRpcClient;
    }

    /**
     * 手动执行任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExecutionRecordBO execute(String taskId) {
        DataWorkTask task = dataWorkTaskRepository.findById(taskId);
        Assert.notNull(task, new SilentException("任务不存在"));

        ExecutionRecordCmd cmd = new ExecutionRecordCmd()
                .setTaskId(taskId)
                .setTaskName(task.getName())
                .setEngineType(task.getEngineType())
                .setSqlContent(task.getSqlContent())
                .setStatus(ExecutionStatus.RUNNING);

        ExecutionRecord record = ExecutionAppConvert.INSTANCE.toExecutionRecord(cmd);
        record.setCreatedAt(LocalDateTime.now());
        record = record.save(executionRecordRepository);

        long startTime = System.currentTimeMillis();
        try {
            String resultData;
            if (task.getEngineType() == EngineType.SPARK) {
                resultData = executeSparkSql(task.getSqlContent());
            } else {
                resultData = executeFlinkSql(task.getSqlContent());
            }
            record.setStatus(ExecutionStatus.SUCCESS);
            record.setResultData(resultData);
        } catch (Exception e) {
            record.setStatus(ExecutionStatus.FAILED);
            record.setErrorMessage(e.getMessage());
        } finally {
            record.setCostTimeMs(System.currentTimeMillis() - startTime);
            record = record.update(executionRecordRepository);
        }

        return ExecutionAppConvert.INSTANCE.toExecutionRecordBO(record);
    }

    /**
     * 执行SparkSQL
     */
    private String executeSparkSql(String sql) {
        SqlExecuteCmd cmd = new SqlExecuteCmd();
        cmd.setSql(sql);
        Response<SqlExecuteResultDTO> response = sqlGatewayClient.executeSparkSql(cmd);
        if (response == null || response.getData() == null) {
            throw new SilentException("SparkSQL执行失败：无响应");
        }
        SqlExecuteResultDTO result = response.getData();
        if (result.getErrorMessage() != null && !result.getErrorMessage().isEmpty()) {
            throw new SilentException("SparkSQL执行失败：" + result.getErrorMessage());
        }
        return JSON.toJSONString(result.getData());
    }

    /**
     * 执行FlinkSQL
     */
    private String executeFlinkSql(String sql) {
        return flinkRpcClient.executeSql(sql);
    }

    /**
     * 分页查询执行记录
     */
    @Override
    public Page<ExecutionRecordBO> page(ExecutionRecordPageQuery query) {
        Page<ExecutionRecord> page = executionRecordRepository.page(query);
        List<ExecutionRecordBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(ExecutionAppConvert.INSTANCE::toExecutionRecordBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据ID查询执行记录
     */
    @Override
    public ExecutionRecordBO findById(String id) {
        ExecutionRecord record = executionRecordRepository.findById(id);
        Assert.notNull(record, new SilentException("执行记录不存在"));
        return ExecutionAppConvert.INSTANCE.toExecutionRecordBO(record);
    }
}
