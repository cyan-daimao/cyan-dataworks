package com.cyan.dataworks.client.job;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job.dto.JobDagDefinitionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * DataWorks 单节点作业 Airflow RPC Feign 客户端
 * <p>
 * 供 Airflow 动态DAG脚本查询单节点作业DAG定义。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "dataWorksJobAirflowClient", path = "/rpc/dataworks", url = "${feign.cyan-dataworks.url:}")
public interface DataWorksJobAirflowClient {

    /**
     * 查询单节点作业DAG定义
     *
     * @return 单节点作业DAG定义列表
     */
    @GetMapping("/airflow/job-dag-definitions")
    Response<List<JobDagDefinitionDTO>> listJobDagDefinitions();
}
