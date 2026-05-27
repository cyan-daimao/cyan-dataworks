package com.cyan.dataworks.adapter.schedule.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.schedule.http.convert.AirflowDagDefinitionAdapterConvert;
import com.cyan.dataworks.adapter.schedule.http.dto.AirflowDagDefinitionDTO;
import com.cyan.dataworks.application.schedule.AirflowDagDefinitionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Airflow DAG定义RPC接口。
 *
 * <p>供Airflow scheduler拉取DAG定义，不依赖登录态。</p>
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/dataworks/airflow")
public class AirflowDagDefinitionRpcController {

    /**
     * Airflow DAG定义应用服务
     */
    private final AirflowDagDefinitionService airflowDagDefinitionService;

    public AirflowDagDefinitionRpcController(AirflowDagDefinitionService airflowDagDefinitionService) {
        this.airflowDagDefinitionService = airflowDagDefinitionService;
    }

    /**
     * 查询启用的DAG定义
     *
     * @return DAG定义列表
     */
    @GetMapping("/dag-definitions")
    public Response<List<AirflowDagDefinitionDTO>> listEnabledDefinitions() {
        List<AirflowDagDefinitionDTO> data = airflowDagDefinitionService.listEnabledDefinitions().stream()
                .map(AirflowDagDefinitionAdapterConvert.INSTANCE::toDTO)
                .toList();
        return Response.success(data);
    }
}
