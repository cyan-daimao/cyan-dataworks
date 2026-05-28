package com.cyan.dataworks.application.job.lineage;

import com.alibaba.fastjson2.JSON;
import com.cyan.dataman.client.lineage.dto.MetadataLineageEdgeDTO;
import com.cyan.dataman.client.lineage.dto.MetadataLineageNodeDTO;
import com.cyan.dataman.client.lineage.request.MetadataLineageSyncRequest;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.infra.gateway.MetadataLineageGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业血缘同步服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class JobLineageSyncService {

    private static final String SERVICE_NAME = "cyan-dataworks";
    private static final String NODE_ETL_JOB = "ETL_JOB";
    private static final String NODE_FIELD = "FIELD";
    private static final String EDGE_READS_FIELD = "READS_FIELD";
    private static final String EDGE_WRITES_FIELD = "WRITES_FIELD";
    private static final String EDGE_SCHEDULE_DEPENDS_ON = "SCHEDULE_DEPENDS_ON";

    private final SqlFieldLineageExtractor sqlFieldLineageExtractor;
    private final MetadataLineageGateway metadataLineageGateway;

    public JobLineageSyncService(SqlFieldLineageExtractor sqlFieldLineageExtractor,
                                 MetadataLineageGateway metadataLineageGateway) {
        this.sqlFieldLineageExtractor = sqlFieldLineageExtractor;
        this.metadataLineageGateway = metadataLineageGateway;
    }

    /**
     * 同步作业字段血缘
     */
    public void sync(Job job) {
        if (job == null || job.getId() == null || job.getId().isBlank()) {
            return;
        }
        SqlFieldLineageExtractor.ExtractResult result = sqlFieldLineageExtractor.extract(job.getContent());
        String jobKey = "etl_job:dataworks:" + job.getId();
        Map<String, MetadataLineageNodeDTO> nodes = new LinkedHashMap<>();
        List<MetadataLineageEdgeDTO> edges = new ArrayList<>();

        nodes.put(jobKey, new MetadataLineageNodeDTO()
                .setNodeKey(jobKey)
                .setNodeType(NODE_ETL_JOB)
                .setNodeName(job.getName())
                .setServiceName(SERVICE_NAME)
                .setRefId(job.getId())
                .setPropertiesJson(result.errorProperties()));

        for (SqlFieldLineageExtractor.FieldRef field : result.inputFields()) {
            String fieldKey = SqlFieldLineageExtractor.fieldKey(field);
            nodes.putIfAbsent(fieldKey, toFieldNode(fieldKey, field));
            edges.add(new MetadataLineageEdgeDTO()
                    .setSourceKey(fieldKey)
                    .setTargetKey(jobKey)
                    .setEdgeType(EDGE_READS_FIELD)
                    .setServiceName(SERVICE_NAME)
                    .setRefId(job.getId()));
        }

        for (SqlFieldLineageExtractor.FieldRef field : result.outputFields()) {
            String fieldKey = SqlFieldLineageExtractor.fieldKey(field);
            nodes.putIfAbsent(fieldKey, toFieldNode(fieldKey, field));
            edges.add(new MetadataLineageEdgeDTO()
                    .setSourceKey(jobKey)
                    .setTargetKey(fieldKey)
                    .setEdgeType(EDGE_WRITES_FIELD)
                    .setServiceName(SERVICE_NAME)
                    .setRefId(job.getId()));
        }

        metadataLineageGateway.sync(new MetadataLineageSyncRequest()
                .setServiceName(SERVICE_NAME)
                .setRefId(job.getId())
                .setNodes(new ArrayList<>(nodes.values()))
                .setEdges(edges));
    }

    /**
     * 同步作业调度依赖血缘
     */
    public void syncDependencies(Job downstreamJob, List<Job> upstreamJobs) {
        if (downstreamJob == null || downstreamJob.getId() == null || downstreamJob.getId().isBlank()) {
            return;
        }
        String downstreamJobKey = jobKey(downstreamJob.getId());
        Map<String, MetadataLineageNodeDTO> nodes = new LinkedHashMap<>();
        List<MetadataLineageEdgeDTO> edges = new ArrayList<>();
        nodes.put(downstreamJobKey, toJobNode(downstreamJob));
        for (Job upstreamJob : upstreamJobs == null ? List.<Job>of() : upstreamJobs) {
            if (upstreamJob == null || upstreamJob.getId() == null || upstreamJob.getId().isBlank()) {
                continue;
            }
            String upstreamJobKey = jobKey(upstreamJob.getId());
            nodes.putIfAbsent(upstreamJobKey, toJobNode(upstreamJob));
            edges.add(new MetadataLineageEdgeDTO()
                    .setSourceKey(upstreamJobKey)
                    .setTargetKey(downstreamJobKey)
                    .setEdgeType(EDGE_SCHEDULE_DEPENDS_ON)
                    .setServiceName(SERVICE_NAME)
                    .setRefId(dependencyRefId(downstreamJob.getId()))
                    .setPropertiesJson(JSON.toJSONString(Map.of("dependencyType", "SCHEDULE"))));
        }
        metadataLineageGateway.sync(new MetadataLineageSyncRequest()
                .setServiceName(SERVICE_NAME)
                .setRefId(dependencyRefId(downstreamJob.getId()))
                .setNodes(new ArrayList<>(nodes.values()))
                .setEdges(edges));
    }

    /**
     * 转换字段节点
     */
    private MetadataLineageNodeDTO toFieldNode(String fieldKey, SqlFieldLineageExtractor.FieldRef field) {
        return new MetadataLineageNodeDTO()
                .setNodeKey(fieldKey)
                .setNodeType(NODE_FIELD)
                .setNodeName(field.column())
                .setServiceName(SERVICE_NAME)
                .setTableRef(SqlFieldLineageExtractor.tableRef(field))
                .setColumnName(field.column());
    }

    /**
     * 转换作业节点
     */
    private MetadataLineageNodeDTO toJobNode(Job job) {
        return new MetadataLineageNodeDTO()
                .setNodeKey(jobKey(job.getId()))
                .setNodeType(NODE_ETL_JOB)
                .setNodeName(job.getName())
                .setServiceName(SERVICE_NAME)
                .setRefId(job.getId())
                .setPropertiesJson(JSON.toJSONString(Map.of(
                        "engineType", job.getEngineType() == null ? "" : job.getEngineType().name(),
                        "nodeType", job.getNodeType() == null ? "" : job.getNodeType().name(),
                        "status", job.getStatus() == null ? "" : job.getStatus().name()
                )));
    }

    /**
     * 作业节点唯一键
     */
    private String jobKey(String jobId) {
        return "etl_job:dataworks:" + jobId;
    }

    /**
     * 依赖血缘来源ID
     */
    private String dependencyRefId(String downstreamJobId) {
        return "job:" + downstreamJobId + ":dependencies";
    }
}
