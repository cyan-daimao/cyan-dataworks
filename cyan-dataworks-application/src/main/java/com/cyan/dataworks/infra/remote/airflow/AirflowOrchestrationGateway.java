package com.cyan.dataworks.infra.remote.airflow;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.workflow.bo.AirflowDagBO;
import com.cyan.dataworks.application.workflow.bo.AirflowDagRunBO;
import com.cyan.dataworks.application.workflow.bo.AirflowTaskInstanceBO;
import com.cyan.dataworks.infra.config.AirflowProperties;
import com.cyan.dataworks.infra.remote.airflow.client.AirflowDagApiClient;
import com.cyan.dataworks.infra.remote.airflow.client.AirflowDagRunApiClient;
import com.cyan.dataworks.infra.remote.airflow.client.AirflowTaskClearApiClient;
import com.cyan.dataworks.infra.remote.airflow.client.AirflowTaskInstanceApiClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Airflow编排网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class AirflowOrchestrationGateway {

    /** Airflow配置 */
    private final AirflowProperties airflowProperties;

    /** DAG API客户端 */
    private final AirflowDagApiClient dagApiClient;

    /** DAG Run API客户端 */
    private final AirflowDagRunApiClient dagRunApiClient;

    /** 任务实例API客户端 */
    private final AirflowTaskInstanceApiClient taskInstanceApiClient;

    /** 任务清理API客户端 */
    private final AirflowTaskClearApiClient taskClearApiClient;

    public AirflowOrchestrationGateway(AirflowProperties airflowProperties,
                                       AirflowDagApiClient dagApiClient,
                                       AirflowDagRunApiClient dagRunApiClient,
                                       AirflowTaskInstanceApiClient taskInstanceApiClient,
                                       AirflowTaskClearApiClient taskClearApiClient) {
        this.airflowProperties = airflowProperties;
        this.dagApiClient = dagApiClient;
        this.dagRunApiClient = dagRunApiClient;
        this.taskInstanceApiClient = taskInstanceApiClient;
        this.taskClearApiClient = taskClearApiClient;
    }

    /** 查询DAG详情 */
    public AirflowDagBO getDag(String dagId) {
        Map<String, Object> payload = call(() -> dagApiClient.getDag(uri("/api/v1/dags/" + encode(dagId)), authorization()));
        return toDagBO(payload);
    }

    /** 同步DAG暂停状态 */
    public void syncDagPaused(String dagId, boolean paused, boolean strict) {
        if (!isEnabled()) {
            return;
        }
        try {
            dagApiClient.patchDag(uri("/api/v1/dags/" + encode(dagId)), authorization(), "is_paused", Map.of("is_paused", paused));
            log.info("Airflow DAG状态同步成功: dagId={}, paused={}", dagId, paused);
        } catch (FeignException.NotFound e) {
            log.warn("Airflow DAG不存在，状态同步跳过: dagId={}, paused={}", dagId, paused);
            if (strict) {
                throw new SilentException("Airflow DAG不存在，请等待调度器刷新后再操作");
            }
        } catch (Exception e) {
            log.warn("Airflow DAG状态同步失败: dagId={}, paused={}, reason={}", dagId, paused, e.getMessage(), e);
            if (strict) {
                throw new SilentException("同步Airflow DAG状态失败: " + e.getMessage());
            }
        }
    }

    /** 删除DAG */
    public void deleteDag(String dagId) {
        if (!isEnabled()) {
            return;
        }
        callVoid(() -> dagApiClient.deleteDag(uri("/api/v1/dags/" + encode(dagId)), authorization()));
    }

    /** 触发DAG Run */
    public AirflowDagRunBO triggerDagRun(String dagId) {
        Map<String, Object> payload = call(() -> dagRunApiClient.triggerDagRun(
                uri("/api/v1/dags/" + encode(dagId) + "/dagRuns"),
                authorization(),
                Map.of("conf", Map.of())
        ));
        return toDagRunBO(payload);
    }

    /** 查询DAG Run列表 */
    public List<AirflowDagRunBO> listDagRuns(String dagId, Integer limit, Integer offset) {
        Map<String, Object> payload = call(() -> dagRunApiClient.listDagRuns(
                uri("/api/v1/dags/" + encode(dagId) + "/dagRuns"),
                authorization(),
                Map.of("limit", Optional.ofNullable(limit).orElse(20), "offset", Optional.ofNullable(offset).orElse(0))
        ));
        return asList(payload.get("dag_runs")).stream().map(this::toDagRunBO).toList();
    }

    /** 查询DAG Run详情 */
    public AirflowDagRunBO getDagRun(String dagId, String dagRunId) {
        Map<String, Object> payload = call(() -> dagRunApiClient.getDagRun(
                uri("/api/v1/dags/" + encode(dagId) + "/dagRuns/" + encode(dagRunId)), authorization()));
        return toDagRunBO(payload);
    }

    /** 更新DAG Run状态 */
    public AirflowDagRunBO updateDagRunState(String dagId, String dagRunId, String state) {
        Map<String, Object> payload = call(() -> dagRunApiClient.patchDagRun(
                uri("/api/v1/dags/" + encode(dagId) + "/dagRuns/" + encode(dagRunId)),
                authorization(),
                "state",
                Map.of("state", state)
        ));
        return toDagRunBO(payload);
    }

    /** 查询任务实例列表 */
    public List<AirflowTaskInstanceBO> listTaskInstances(String dagId, String dagRunId) {
        Map<String, Object> payload = call(() -> taskInstanceApiClient.listTaskInstances(
                uri("/api/v1/dags/" + encode(dagId) + "/dagRuns/" + encode(dagRunId) + "/taskInstances"),
                authorization(),
                Map.of()
        ));
        return asList(payload.get("task_instances")).stream().map(this::toTaskInstanceBO).toList();
    }

    /** 重跑任务实例 */
    public void rerunTaskInstance(String dagId, String dagRunId, String taskId) {
        call(() -> taskClearApiClient.clearTaskInstances(
                uri("/api/v1/dags/" + encode(dagId) + "/clearTaskInstances"),
                authorization(),
                Map.of("dry_run", false, "dag_run_id", dagRunId, "task_ids", List.of(taskId))
        ));
    }

    /** 更新任务实例状态 */
    public AirflowTaskInstanceBO updateTaskInstanceState(String dagId, String dagRunId, String taskId, String state) {
        Map<String, Object> payload = call(() -> taskInstanceApiClient.patchTaskInstance(
                uri("/api/v1/dags/" + encode(dagId) + "/dagRuns/" + encode(dagRunId) + "/taskInstances/" + encode(taskId)),
                authorization(),
                "state",
                Map.of("state", state)
        ));
        return toTaskInstanceBO(payload);
    }

    /** 构建DAG ID */
    public String buildWorkflowDagId(String workflowId) {
        String prefix = Optional.ofNullable(airflowProperties.getDagPrefix()).filter(value -> !value.isBlank()).orElse("dataworks");
        return prefix + "_workflow_" + workflowId;
    }

    /** 构建单节点作业DAG ID */
    public String buildJobDagId(String jobId) {
        String prefix = Optional.ofNullable(airflowProperties.getDagPrefix()).filter(value -> !value.isBlank()).orElse("dataworks");
        return prefix + "_job_" + jobId;
    }

    private AirflowDagBO toDagBO(Map<String, Object> payload) {
        return new AirflowDagBO()
                .setDagId(str(payload.get("dag_id")))
                .setPaused(bool(payload.get("is_paused")))
                .setActive(bool(payload.get("is_active")));
    }

    private AirflowDagRunBO toDagRunBO(Map<String, Object> payload) {
        return new AirflowDagRunBO()
                .setDagId(str(payload.get("dag_id")))
                .setDagRunId(str(payload.get("dag_run_id")))
                .setState(str(payload.get("state")))
                .setLogicalDate(str(payload.get("logical_date")))
                .setStartDate(str(payload.get("start_date")))
                .setEndDate(str(payload.get("end_date")));
    }

    private AirflowTaskInstanceBO toTaskInstanceBO(Map<String, Object> payload) {
        return new AirflowTaskInstanceBO()
                .setDagId(str(payload.get("dag_id")))
                .setDagRunId(str(payload.get("dag_run_id")))
                .setTaskId(str(payload.get("task_id")))
                .setState(str(payload.get("state")))
                .setTryNumber(payload.get("try_number") instanceof Number number ? number.intValue() : null)
                .setStartDate(str(payload.get("start_date")))
                .setEndDate(str(payload.get("end_date")));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
        }
        return List.of();
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : null;
    }

    private URI uri(String path) {
        return URI.create(trimTrailingSlash(airflowProperties.getBaseUrl()) + path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private <T> T call(AirflowCall<T> call) {
        if (!isEnabled()) {
            throw new SilentException("Airflow集成未启用");
        }
        try {
            return call.execute();
        } catch (SilentException e) {
            throw e;
        } catch (Exception e) {
            throw new SilentException("调用Airflow失败: " + e.getMessage());
        }
    }

    private void callVoid(AirflowVoidCall call) {
        call(() -> {
            call.execute();
            return Map.of();
        });
    }

    private boolean isEnabled() {
        return Boolean.TRUE.equals(airflowProperties.getEnabled())
                && airflowProperties.getBaseUrl() != null
                && !airflowProperties.getBaseUrl().isBlank();
    }

    private String authorization() {
        if (airflowProperties.getToken() != null && !airflowProperties.getToken().isBlank()) {
            return "Bearer " + airflowProperties.getToken();
        }
        if (airflowProperties.getUsername() == null || airflowProperties.getUsername().isBlank()) {
            return null;
        }
        String raw = airflowProperties.getUsername() + ":" + Optional.ofNullable(airflowProperties.getPassword()).orElse("");
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private interface AirflowCall<T> {
        T execute();
    }

    private interface AirflowVoidCall {
        void execute();
    }
}
