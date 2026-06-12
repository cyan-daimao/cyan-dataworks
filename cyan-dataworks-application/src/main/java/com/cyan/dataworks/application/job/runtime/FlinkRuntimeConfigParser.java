package com.cyan.dataworks.application.job.runtime;

import com.cyan.dataworks.domain.job.Job;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flink运行配置解析器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class FlinkRuntimeConfigParser {

    /**
     * 默认TaskManager内存
     */
    private static final int DEFAULT_TASK_MANAGER_MEMORY_GB = 1;

    /**
     * 默认TaskManager CPU
     */
    private static final double DEFAULT_TASK_MANAGER_CPU = 0.5D;

    /**
     * 默认JobManager内存
     */
    private static final int DEFAULT_JOB_MANAGER_MEMORY_GB = 1;

    /**
     * 默认JobManager CPU
     */
    private static final double DEFAULT_JOB_MANAGER_CPU = 0.5D;

    /**
     * 默认 Flink 版本
     */
    private static final String DEFAULT_FLINK_VERSION = "v2_0";

    /**
     * 默认升级模式
     */
    private static final String DEFAULT_UPGRADE_MODE = "last-state";

    /**
     * 默认期望状态
     */
    private static final String DEFAULT_STATE = "running";

    /**
     * 默认 checkpoint 配置
     */
    private static final String DEFAULT_CHECKPOINT_INTERVAL = "60s";
    private static final String DEFAULT_CHECKPOINT_TIMEOUT = "600s";
    private static final int DEFAULT_CHECKPOINT_MAX_CONCURRENT = 1;
    private static final String DEFAULT_CHECKPOINT_MIN_PAUSE = "500ms";
    private static final String DEFAULT_CHECKPOINT_MODE = "EXACTLY_ONCE";

    /**
     * 默认状态后端
     */
    private static final String DEFAULT_STATE_BACKEND_TYPE = "rocksdb";

    /**
     * JSON解析器
     */
    private final ObjectMapper objectMapper;

    /**
     * 默认并行度
     */
    @Value("${flink.operator.parallelism:1}")
    private Integer defaultParallelism;

    public FlinkRuntimeConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析作业运行配置
     */
    public FlinkRuntimeConfig parse(Job job) {
        FlinkRuntimeConfig defaults = defaults();
        if (job == null || job.getConfigJson() == null || job.getConfigJson().isBlank()) {
            return defaults;
        }
        try {
            JsonNode root = objectMapper.readTree(job.getConfigJson());
            JsonNode flink = root.path("flink");
            return new FlinkRuntimeConfig()
                    .setTaskManagerMemoryGb(normalizeMemoryGb(flink.path("taskManagerMemoryGb").asInt(defaults.getTaskManagerMemoryGb()), DEFAULT_TASK_MANAGER_MEMORY_GB))
                    .setTaskManagerCpu(normalizeCpu(flink.path("taskManagerCpu").asDouble(defaults.getTaskManagerCpu()), DEFAULT_TASK_MANAGER_CPU))
                    .setParallelism(normalizeParallelism(flink.path("parallelism").asInt(defaults.getParallelism())))
                    .setJobManagerMemoryGb(normalizeMemoryGb(flink.path("jobManagerMemoryGb").asInt(defaults.getJobManagerMemoryGb()), DEFAULT_JOB_MANAGER_MEMORY_GB))
                    .setJobManagerCpu(normalizeCpu(flink.path("jobManagerCpu").asDouble(defaults.getJobManagerCpu()), DEFAULT_JOB_MANAGER_CPU))
                    .setFlinkVersion(textOrDefault(flink.path("flinkVersion"), defaults.getFlinkVersion()))
                    .setUpgradeMode(textOrDefault(flink.path("upgradeMode"), defaults.getUpgradeMode()))
                    .setState(textOrDefault(flink.path("state"), defaults.getState()))
                    .setCheckpointInterval(textOrDefault(flink.path("checkpointInterval"), defaults.getCheckpointInterval()))
                    .setCheckpointTimeout(textOrDefault(flink.path("checkpointTimeout"), defaults.getCheckpointTimeout()))
                    .setCheckpointMaxConcurrent(normalizePositive(flink.path("checkpointMaxConcurrent").asInt(defaults.getCheckpointMaxConcurrent()), DEFAULT_CHECKPOINT_MAX_CONCURRENT))
                    .setCheckpointMinPause(textOrDefault(flink.path("checkpointMinPause"), defaults.getCheckpointMinPause()))
                    .setCheckpointMode(textOrDefault(flink.path("checkpointMode"), defaults.getCheckpointMode()))
                    .setStateBackendType(textOrDefault(flink.path("stateBackendType"), defaults.getStateBackendType()))
                    .setExtraFlinkConfiguration(parseExtraConfiguration(flink.path("flinkConfiguration")));
        } catch (Exception e) {
            log.warn("解析Flink运行配置失败，使用默认配置: jobId={}, error={}", job.getId(), e.getMessage());
            return defaults;
        }
    }

    /**
     * 默认配置
     */
    private FlinkRuntimeConfig defaults() {
        return new FlinkRuntimeConfig()
                .setTaskManagerMemoryGb(DEFAULT_TASK_MANAGER_MEMORY_GB)
                .setTaskManagerCpu(DEFAULT_TASK_MANAGER_CPU)
                .setParallelism(normalizeParallelism(defaultParallelism))
                .setJobManagerMemoryGb(DEFAULT_JOB_MANAGER_MEMORY_GB)
                .setJobManagerCpu(DEFAULT_JOB_MANAGER_CPU)
                .setFlinkVersion(DEFAULT_FLINK_VERSION)
                .setUpgradeMode(DEFAULT_UPGRADE_MODE)
                .setState(DEFAULT_STATE)
                .setCheckpointInterval(DEFAULT_CHECKPOINT_INTERVAL)
                .setCheckpointTimeout(DEFAULT_CHECKPOINT_TIMEOUT)
                .setCheckpointMaxConcurrent(DEFAULT_CHECKPOINT_MAX_CONCURRENT)
                .setCheckpointMinPause(DEFAULT_CHECKPOINT_MIN_PAUSE)
                .setCheckpointMode(DEFAULT_CHECKPOINT_MODE)
                .setStateBackendType(DEFAULT_STATE_BACKEND_TYPE)
                .setExtraFlinkConfiguration(new LinkedHashMap<>());
    }

    /**
     * 规整内存（GB）
     */
    private Integer normalizeMemoryGb(Integer value, int fallback) {
        if (value == null || value < 1) {
            return fallback;
        }
        return value;
    }

    /**
     * 规整CPU
     */
    private Double normalizeCpu(Double value, double fallback) {
        if (value == null || value < 0.1D) {
            return fallback;
        }
        return value;
    }

    /**
     * 规整并行度
     */
    private Integer normalizeParallelism(Integer value) {
        if (value == null || value < 1) {
            return 1;
        }
        return value;
    }

    /**
     * 规整正整数
     */
    private Integer normalizePositive(Integer value, int fallback) {
        if (value == null || value < 1) {
            return fallback;
        }
        return value;
    }

    /**
     * 取文本值，空则使用默认
     */
    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String text = node.asText("");
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        return text;
    }

    /**
     * 解析自定义 flinkConfiguration
     */
    private Map<String, String> parseExtraConfiguration(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value == null || value.isNull() || value.isMissingNode()) {
                continue;
            }
            result.put(entry.getKey(), value.asText());
        }
        return result;
    }
}
