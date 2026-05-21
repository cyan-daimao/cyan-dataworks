package com.cyan.dataworks.client.job_instance.request;

import com.cyan.dataworks.client.enums.EngineType;
import com.cyan.dataworks.client.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 临时执行作业请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobPreviewExecuteRequest {

    /**
     * 作业名称
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
     * SQL内容
     */
    private String sqlContent;

    /**
     * 节点配置JSON
     */
    private String configJson;
}
