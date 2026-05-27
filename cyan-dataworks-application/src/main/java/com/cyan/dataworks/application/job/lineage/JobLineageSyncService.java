package com.cyan.dataworks.application.job.lineage;

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
}
