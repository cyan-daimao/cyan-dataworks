package com.cyan.dataworks.client.job.query;

import com.cyan.dataworks.client.enums.EngineType;
import com.cyan.dataworks.client.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据加工作业分页查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobPageQuery {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 作业名称（模糊查询）
     */
    private String name;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * 节点类型
     */
    private NodeType nodeType;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 文件夹ID
     */
    private Long folderId;
}
