package com.cyan.dataworks.adapter.workflow.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job_instance.http.convert.JobInstanceAdapterConvert;
import com.cyan.dataworks.adapter.workflow.http.convert.WorkflowAdapterConvert;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.workflow.WorkflowRunService;
import com.cyan.dataworks.application.workflow.WorkflowService;
import com.cyan.dataworks.application.workflow.cmd.WorkflowRunBySchedulerCmd;
import com.cyan.dataworks.client.job_instance.dto.JobInstanceDTO;
import com.cyan.dataworks.client.workflow.DataWorksWorkflowAirflowClient;
import com.cyan.dataworks.client.workflow.dto.WorkflowDagDefinitionDTO;
import com.cyan.dataworks.client.workflow.request.WorkflowRunBySchedulerRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流Airflow RPC接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/dataworks")
public class WorkflowAirflowRpcController implements DataWorksWorkflowAirflowClient {

    /** 工作流应用服务 */
    private final WorkflowService workflowService;

    /** 工作流运行服务 */
    private final WorkflowRunService workflowRunService;

    public WorkflowAirflowRpcController(WorkflowService workflowService,
                                        WorkflowRunService workflowRunService) {
        this.workflowService = workflowService;
        this.workflowRunService = workflowRunService;
    }

    /** 查询工作流DAG定义 */
    @GetMapping("/airflow/workflow-dag-definitions")
    @Override
    public Response<List<WorkflowDagDefinitionDTO>> listWorkflowDagDefinitions() {
        return Response.success(workflowService.listAirflowDagDefinitions().stream()
                .map(WorkflowAdapterConvert.INSTANCE::toRpcDagDefinitionDTO).toList());
    }

    /** Airflow触发工作流节点执行 */
    @PostMapping("/workflows/{workflowId}/nodes/{nodeId}/run-by-scheduler")
    @Override
    public Response<JobInstanceDTO> runNodeByScheduler(@PathVariable String workflowId,
                                                       @PathVariable String nodeId,
                                                       @RequestBody @Valid WorkflowRunBySchedulerRequest request) {
        WorkflowRunBySchedulerCmd cmd = WorkflowAdapterConvert.INSTANCE.toWorkflowRunBySchedulerCmd(request);
        JobInstanceBO bo = workflowRunService.runNodeByScheduler(workflowId, nodeId, cmd);
        return Response.success(JobInstanceAdapterConvert.INSTANCE.toRpcJobInstanceDTO(bo));
    }
}
