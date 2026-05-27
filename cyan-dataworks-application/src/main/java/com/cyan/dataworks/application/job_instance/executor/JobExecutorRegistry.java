package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 作业执行器注册表
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class JobExecutorRegistry {

    /**
     * 作业执行器列表
     */
    private final List<JobExecutor> executors;

    public JobExecutorRegistry(List<JobExecutor> executors) {
        this.executors = executors;
    }

    /**
     * 根据节点类型获取执行器
     *
     * @param nodeType 节点类型
     * @return 作业执行器
     */
    public JobExecutor get(NodeType nodeType) {
        return executors.stream()
                .filter(executor -> executor.supports(nodeType))
                .findFirst()
                .orElseThrow(() -> new SilentException("不支持的节点类型：" + nodeType));
    }
}
