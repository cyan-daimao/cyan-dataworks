package com.cyan.dataworks.client.job.request;

import com.cyan.dataworks.client.enums.EngineType;
import com.cyan.dataworks.client.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据加工作业更新请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobUpdateRequest {

    /**
     * 作业名称
     */
    private String name;

    /**
     * 作业描述
     */
    private String description;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * 节点类型
     */
    private NodeType nodeType;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 节点配置JSON
     */
    private String configJson;
}
