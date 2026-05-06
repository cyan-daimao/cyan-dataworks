package com.cyan.dataworks.adapter.job.http.dto;

import com.cyan.dataworks.enums.EngineType;
import com.cyan.dataworks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据加工作业 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 文件夹ID
     */
    private Long folderId;

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
     * SQL内容
     */
    private String sqlContent;

    /**
     * 作业状态
     */
    private TaskStatus status;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
