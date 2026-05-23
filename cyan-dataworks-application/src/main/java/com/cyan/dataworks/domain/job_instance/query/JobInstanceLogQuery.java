package com.cyan.dataworks.domain.job_instance.query;

import com.cyan.dataworks.enums.JobLogRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 作业实例日志查询条件
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceLogQuery {

    /**
     * 日志角色
     */
    private JobLogRole role;

    /**
     * 日志尾部行数
     */
    private Integer tailLines;

    /**
     * 是否读取上一个已终止容器日志
     */
    private Boolean previous;
}
