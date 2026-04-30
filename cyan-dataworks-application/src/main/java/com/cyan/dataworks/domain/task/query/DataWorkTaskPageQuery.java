package com.cyan.dataworks.domain.task.query;

import com.cyan.dataworks.enums.EngineType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据加工任务分页查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DataWorkTaskPageQuery {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 任务名称（模糊查询）
     */
    private String name;

    /**
     * 引擎类型
     */
    private EngineType engineType;

    /**
     * 创建人
     */
    private String createdBy;
}
