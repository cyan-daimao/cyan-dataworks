package com.cyan.dataworks.client.workflow;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO;
import com.cyan.dataworks.client.workflow.dto.WorkflowDagDefinitionDTO;
import com.cyan.dataworks.client.workflow.request.WorkflowRunBySchedulerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * DataWorks 工作流 Airflow RPC Feign 客户端
 * <p>
 * 供 Airflow 调度器回调，路径为 /rpc/dataworks，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataworks", contextId = "dataWorksWorkflowAirflowClient", path = "/rpc/dataworks", url = "${feign.cyan-dataworks.url:}")
public interface DataWorksWorkflowAirflowClient {

    /**
     * 查询工作流DAG定义
     *
     * @return 工作流DAG定义列表
     */
    @GetMapping("/airflow/workflow-dag-definitions")
    Response<List<WorkflowDagDefinitionDTO>> listWorkflowDagDefinitions();

    /**
     * Airflow触发工作流节点执行
     *
     * @param workflowId 工作流ID
     * @param nodeId 工作流节点ID
     * @param request 调度器执行请求
     * @return 作业实例
     */
    @PostMapping("/workflows/{workflowId}/nodes/{nodeId}/run-by-scheduler")
    Response<JobInstanceDTO> runNodeByScheduler(@PathVariable String workflowId,
                                                @PathVariable String nodeId,
                                                @RequestBody WorkflowRunBySchedulerRequest request);
}
