package com.cyan.dataworks.infra.remote.flink.operator.bo;

import com.cyan.dataworks.enums.JobLogRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Flink Pod日志结果
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FlinkPodLogBO {

    /**
     * Pod名称
     */
    private String podName;

    /**
     * Pod角色
     */
    private JobLogRole role;

    /**
     * 容器名称
     */
    private String containerName;

    /**
     * 日志内容
     */
    private String log;
}
