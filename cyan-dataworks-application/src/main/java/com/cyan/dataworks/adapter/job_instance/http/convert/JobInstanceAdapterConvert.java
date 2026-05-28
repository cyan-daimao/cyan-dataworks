package com.cyan.dataworks.adapter.job_instance.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataworks.adapter.job_instance.http.dto.JobInstanceDTO;
import com.cyan.dataworks.adapter.job_instance.http.dto.JobInstanceLogDTO;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceLogBO;
import com.cyan.dataworks.application.job_instance.cmd.JobRunBySchedulerCmd;
import com.cyan.dataworks.client.job_instance.request.JobRunBySchedulerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据加工作业实例适配器层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface JobInstanceAdapterConvert {

    JobInstanceAdapterConvert INSTANCE = Mappers.getMapper(JobInstanceAdapterConvert.class);

    /**
     * BO 转 DTO
     */
    JobInstanceDTO toJobInstanceDTO(JobInstanceBO jobInstanceBO);

    /**
     * BO 转 RPC DTO
     */
    com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO toRpcJobInstanceDTO(JobInstanceBO jobInstanceBO);

    /**
     * RPC调度请求转应用命令
     */
    JobRunBySchedulerCmd toJobRunBySchedulerCmd(JobRunBySchedulerRequest request);

    /**
     * 日志BO 转 DTO
     */
    JobInstanceLogDTO toJobInstanceLogDTO(JobInstanceLogBO jobInstanceLogBO);
}
