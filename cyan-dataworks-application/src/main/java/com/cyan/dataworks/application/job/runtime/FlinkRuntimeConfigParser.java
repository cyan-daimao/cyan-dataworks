package com.cyan.dataworks.application.job.runtime;

import com.cyan.dataworks.domain.job.Job;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
                    .setTaskManagerMemoryGb(normalizeMemoryGb(flink.path("taskManagerMemoryGb").asInt(defaults.getTaskManagerMemoryGb())))
                    .setTaskManagerCpu(normalizeCpu(flink.path("taskManagerCpu").asDouble(defaults.getTaskManagerCpu())))
                    .setParallelism(normalizeParallelism(flink.path("parallelism").asInt(defaults.getParallelism())));
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
                .setParallelism(normalizeParallelism(defaultParallelism));
    }

    /**
     * 规整TaskManager内存
     */
    private Integer normalizeMemoryGb(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_TASK_MANAGER_MEMORY_GB;
        }
        return value;
    }

    /**
     * 规整TaskManager CPU
     */
    private Double normalizeCpu(Double value) {
        if (value == null || value < 0.1D) {
            return DEFAULT_TASK_MANAGER_CPU;
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
}
