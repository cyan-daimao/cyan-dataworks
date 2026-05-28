package com.cyan.dataworks.infra.remote.airflow;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.infra.config.AirflowProperties;
import com.cyan.dataworks.infra.remote.airflow.client.AirflowDagClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Airflow DAG状态同步服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class AirflowDagStateService {

    /**
     * Airflow配置
     */
    private final AirflowProperties airflowProperties;

    /**
     * Airflow DAG客户端
     */
    private final AirflowDagClient airflowDagClient;

    public AirflowDagStateService(AirflowProperties airflowProperties,
                                  AirflowDagClient airflowDagClient) {
        this.airflowProperties = airflowProperties;
        this.airflowDagClient = airflowDagClient;
    }

    /**
     * 同步作业DAG暂停状态
     */
    public void syncJobDagPaused(Job job, boolean paused, boolean strict) {
        if (job == null || job.getId() == null || job.getId().isBlank()) {
            return;
        }
        syncDagPaused(buildDagId(job.getId()), paused, strict);
    }

    /**
     * 同步DAG暂停状态
     */
    public void syncDagPaused(String dagId, boolean paused, boolean strict) {
        if (!isEnabled()) {
            return;
        }
        try {
            String baseUrl = airflowProperties.getBaseUrl();
            URI uri = URI.create(trimTrailingSlash(baseUrl) + "/api/v1/dags/" + dagId);
            airflowDagClient.patchDag(uri, authorization(), "is_paused", Map.of("is_paused", paused));
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

    /**
     * 构建DAG ID
     */
    public String buildDagId(String jobId) {
        String prefix = Optional.ofNullable(airflowProperties.getDagPrefix())
                .filter(value -> !value.isBlank())
                .orElse("dataworks");
        return prefix + "_job_" + jobId;
    }

    /**
     * 判断Airflow同步是否启用
     */
    private boolean isEnabled() {
        return Boolean.TRUE.equals(airflowProperties.getEnabled())
                && airflowProperties.getBaseUrl() != null
                && !airflowProperties.getBaseUrl().isBlank();
    }

    /**
     * 构建认证信息
     */
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

    /**
     * 去掉URL末尾斜杠
     */
    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
