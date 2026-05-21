package com.cyan.dataworks.application.job_instance.cmd;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 临时执行作业命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobPreviewExecuteCmd {

    /**
     * 作业名称
     */
    private String name;

    /**
     * 引擎类型
     */
    @NotNull(message = "引擎类型不能为空")
    private EngineType engineType;

    /**
     * 节点类型
     */
    @NotNull(message = "节点类型不能为空")
    private NodeType nodeType;

    /**
     * SQL内容
     */
    @NotBlank(message = "SQL内容不能为空")
    private String sqlContent;

    /**
     * 节点配置JSON
     */
    private String configJson;
}
