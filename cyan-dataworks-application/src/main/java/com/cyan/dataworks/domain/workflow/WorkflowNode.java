package com.cyan.dataworks.domain.workflow;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流节点领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowNode {

    /**
     * 主键
     */
    private String id;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * 节点类型
     */
    private NodeType nodeType;

    /**
     * 节点内容
     */
    private String content;

    /**
     * X坐标
     */
    private Integer positionX;

    /**
     * Y坐标
     */
    private Integer positionY;

    /**
     * 节点配置JSON
     */
    private String configJson;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 校验节点定义
     */
    public void validateDefinition() {
        Assert.notBlank(this.workflowId, new SilentException("工作流ID不能为空"));
        Assert.notBlank(this.nodeCode, new SilentException("节点编码不能为空"));
        Assert.notBlank(this.nodeName, new SilentException("节点名称不能为空"));
        Assert.notNull(this.engineType, new SilentException("节点引擎类型不能为空"));
        Assert.notNull(this.nodeType, new SilentException("节点类型不能为空"));
        Assert.notBlank(this.content, new SilentException("节点内容不能为空"));
    }
}
