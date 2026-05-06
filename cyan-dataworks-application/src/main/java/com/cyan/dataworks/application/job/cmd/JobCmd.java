package com.cyan.dataworks.application.job.cmd;

import com.cyan.dataworks.enums.EngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据加工作业命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobCmd {

    /**
     * 作业名称
     */
    @NotBlank(message = "作业名称不能为空")
    private String name;

    /**
     * 作业描述
     */
    private String description;

    /**
     * 引擎类型
     */
    @NotNull(message = "引擎类型不能为空")
    private EngineType engineType;

    /**
     * SQL内容
     */
    @NotBlank(message = "SQL内容不能为空")
    private String sqlContent;
}
