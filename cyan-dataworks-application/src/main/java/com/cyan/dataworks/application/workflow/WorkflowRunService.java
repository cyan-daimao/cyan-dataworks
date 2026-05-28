package com.cyan.dataworks.application.workflow;

import com.cyan.arch.common.api.Page;
import com.cyan.dataworks.application.job_instance.bo.JobInstanceBO;
import com.cyan.dataworks.application.workflow.bo.AirflowDagRunBO;
import com.cyan.dataworks.application.workflow.bo.AirflowTaskInstanceBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowInstanceBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowRunBySchedulerCmd;
import com.cyan.dataworks.domain.workflow.query.WorkflowInstancePageQuery;

import java.util.List;

/**
 * 工作流运行应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface WorkflowRunService {

    /** 触发工作流运行 */
    WorkflowInstanceBO trigger(String workflowId, String operator);

    /** 分页查询本地工作流实例 */
    Page<WorkflowInstanceBO> pageInstances(WorkflowInstancePageQuery query);

    /** 查询DAG Run列表 */
    List<AirflowDagRunBO> listDagRuns(String workflowId, Integer limit, Integer offset);

    /** 查询DAG Run详情 */
    AirflowDagRunBO getDagRun(String workflowId, String dagRunId);

    /** 更新DAG Run状态 */
    AirflowDagRunBO updateDagRunState(String workflowId, String dagRunId, String state);

    /** 查询任务实例列表 */
    List<AirflowTaskInstanceBO> listTaskInstances(String workflowId, String dagRunId);

    /** 重跑任务实例 */
    void rerunTaskInstance(String workflowId, String dagRunId, String taskId);

    /** 更新任务实例状态 */
    AirflowTaskInstanceBO updateTaskInstanceState(String workflowId, String dagRunId, String taskId, String state);

    /** 调度器触发节点执行 */
    JobInstanceBO runNodeByScheduler(String workflowId, String nodeId, WorkflowRunBySchedulerCmd cmd);
}
