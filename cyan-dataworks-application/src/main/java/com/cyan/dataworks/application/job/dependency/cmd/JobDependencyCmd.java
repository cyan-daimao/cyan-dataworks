package com.cyan.dataworks.application.job.dependency.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 作业依赖命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobDependencyCmd {

    /**
     * 上游作业ID列表
     */
    private List<String> upstreamJobIds;
}
