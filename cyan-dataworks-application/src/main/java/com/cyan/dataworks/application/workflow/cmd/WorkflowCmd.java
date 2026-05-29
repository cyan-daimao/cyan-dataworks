package com.cyan.dataworks.application.workflow.cmd;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 工作流命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowCmd {

    /** 工作流名称 */
    @NotBlank(message = "工作流名称不能为空")
    private String name;

    /** 工作流描述 */
    private String description;

}
