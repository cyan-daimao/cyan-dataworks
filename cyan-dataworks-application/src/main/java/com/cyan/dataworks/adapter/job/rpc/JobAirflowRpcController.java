package com.cyan.dataworks.adapter.job.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.job.http.convert.JobAdapterConvert;
import com.cyan.dataworks.application.job.JobService;
import com.cyan.dataworks.client.job.DataWorksJobAirflowClient;
import com.cyan.dataworks.client.job.dto.JobDagDefinitionDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 单节点作业 Airflow RPC 接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/dataworks")
public class JobAirflowRpcController implements DataWorksJobAirflowClient {

    /** 作业应用服务 */
    private final JobService jobService;

    public JobAirflowRpcController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * 查询单节点作业DAG定义
     */
    @GetMapping("/airflow/job-dag-definitions")
    @Override
    public Response<List<JobDagDefinitionDTO>> listJobDagDefinitions() {
        return Response.success(jobService.listAirflowDagDefinitions().stream()
                .map(JobAdapterConvert.INSTANCE::toRpcJobDagDefinitionDTO)
                .toList());
    }
}
