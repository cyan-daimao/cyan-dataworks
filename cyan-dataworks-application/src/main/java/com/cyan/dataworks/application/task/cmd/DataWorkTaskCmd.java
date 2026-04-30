package com.cyan.dataworks.application.task.cmd;

import com.cyan.dataworks.enums.EngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据加工任务命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DataWorkTaskCmd {

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    private String name;

    /**
     * 任务描述
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
