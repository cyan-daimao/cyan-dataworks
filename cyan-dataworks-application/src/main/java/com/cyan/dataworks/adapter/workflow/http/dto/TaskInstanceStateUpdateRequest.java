package com.cyan.dataworks.adapter.workflow.http.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 任务实例状态更新请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TaskInstanceStateUpdateRequest {

    /** 状态 */
    @NotBlank(message = "状态不能为空")
    private String state;
}
