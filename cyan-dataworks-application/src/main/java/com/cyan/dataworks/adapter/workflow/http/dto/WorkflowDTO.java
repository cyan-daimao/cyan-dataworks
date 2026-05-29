package com.cyan.dataworks.adapter.workflow.http.dto;

import com.cyan.dataworks.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkflowDTO {

    /** 主键 */
    private String id;

    /** 工作流名称 */
    private String name;

    /** 工作流描述 */
    private String description;

    /** DAG ID */
    private String dagId;

    /** 工作流状态 */
    private TaskStatus status;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
