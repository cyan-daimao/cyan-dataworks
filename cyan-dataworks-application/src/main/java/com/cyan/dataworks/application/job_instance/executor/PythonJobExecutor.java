package com.cyan.dataworks.application.job_instance.executor;

import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.enums.NodeType;
import com.cyan.dataworks.infra.remote.kubernetes.ScriptKubernetesJobService;
import org.springframework.stereotype.Component;

/**
 * Python作业执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class PythonJobExecutor implements JobExecutor {

    /**
     * Kubernetes脚本运行服务
     */
    private final ScriptKubernetesJobService scriptKubernetesJobService;

    public PythonJobExecutor(ScriptKubernetesJobService scriptKubernetesJobService) {
        this.scriptKubernetesJobService = scriptKubernetesJobService;
    }

    /**
     * 是否支持节点类型
     */
    @Override
    public boolean supports(NodeType nodeType) {
        return nodeType == NodeType.PYTHON;
    }

    /**
     * 执行Python脚本
     */
    @Override
    public JobExecutionResult execute(Job job, JobInstance instance) {
        if ("preview".equals(instance.getId())) {
            return new JobExecutionResult().setResultData(scriptKubernetesJobService.runPython(instance.getId(), job.getContent(), job.getConfigJson()));
        }
        String runtimeJobName = scriptKubernetesJobService.submitPython(instance.getId(), job.getContent(), job.getConfigJson());
        return new JobExecutionResult()
                .setAsyncSubmitted(true)
                .setRuntimeJobName(runtimeJobName);
    }
}
