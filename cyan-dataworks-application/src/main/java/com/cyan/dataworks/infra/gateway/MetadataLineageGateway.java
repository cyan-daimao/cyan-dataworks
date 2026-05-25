package com.cyan.dataworks.infra.gateway;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.lineage.MetadataLineageClient;
import com.cyan.dataman.client.lineage.request.MetadataLineageSyncRequest;
import com.cyan.dataman.client.table.MetadataTableClient;
import com.cyan.dataman.client.table.dto.MetadataColumnDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 元数据血缘网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class MetadataLineageGateway {

    private final MetadataLineageClient metadataLineageClient;
    private final MetadataTableClient metadataTableClient;

    public MetadataLineageGateway(MetadataLineageClient metadataLineageClient,
                                  MetadataTableClient metadataTableClient) {
        this.metadataLineageClient = metadataLineageClient;
        this.metadataTableClient = metadataTableClient;
    }

    /**
     * 同步血缘
     */
    public void sync(MetadataLineageSyncRequest request) {
        try {
            metadataLineageClient.sync(request);
        } catch (Exception e) {
            log.warn("同步 dataworks 血缘失败, serviceName={}, refId={}", request.getServiceName(), request.getRefId(), e);
        }
    }

    /**
     * 查询表字段
     */
    public List<MetadataColumnDTO> listColumns(String catalog, String schema, String table) {
        try {
            Response<List<MetadataColumnDTO>> response = metadataTableClient.listColumns(catalog, schema, table);
            if (response == null || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("查询元数据字段失败, catalog={}, schema={}, table={}", catalog, schema, table, e);
            return List.of();
        }
    }
}
