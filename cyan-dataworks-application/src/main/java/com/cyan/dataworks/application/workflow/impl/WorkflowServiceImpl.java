package com.cyan.dataworks.application.workflow.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.workflow.WorkflowService;
import com.cyan.dataworks.application.workflow.bo.WorkflowBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDagDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDefinitionBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowDependencyBO;
import com.cyan.dataworks.application.workflow.bo.WorkflowScheduleBO;
import com.cyan.dataworks.application.workflow.cmd.WorkflowCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowDefinitionCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowDependencyCmd;
import com.cyan.dataworks.application.workflow.cmd.WorkflowScheduleCmd;
import com.cyan.dataworks.application.workflow.convert.WorkflowAppConvert;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.domain.workflow.Workflow;
import com.cyan.dataworks.domain.workflow.WorkflowDependency;
import com.cyan.dataworks.domain.workflow.WorkflowEdge;
import com.cyan.dataworks.domain.workflow.WorkflowNode;
import com.cyan.dataworks.domain.workflow.WorkflowSchedule;
import com.cyan.dataworks.domain.workflow.query.WorkflowPageQuery;
import com.cyan.dataworks.domain.workflow.repository.WorkflowDependencyRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowEdgeRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowNodeRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowRepository;
import com.cyan.dataworks.domain.workflow.repository.WorkflowScheduleRepository;
import com.cyan.dataworks.enums.JobDependencyType;
import com.cyan.dataworks.enums.SchedulerType;
import com.cyan.dataworks.enums.TaskStatus;
import com.cyan.dataworks.enums.WorkflowType;
import com.cyan.dataworks.infra.remote.airflow.AirflowOrchestrationGateway;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * 工作流应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

    /** 工作流仓储 */
    private final WorkflowRepository workflowRepository;

    /** 工作流节点仓储 */
    private final WorkflowNodeRepository workflowNodeRepository;

    /** 工作流依赖边仓储 */
    private final WorkflowEdgeRepository workflowEdgeRepository;

    /** 工作流级依赖仓储 */
    private final WorkflowDependencyRepository workflowDependencyRepository;

    /** 工作流调度仓储 */
    private final WorkflowScheduleRepository workflowScheduleRepository;

    /** 作业仓储 */
    private final JobRepository jobRepository;

    /** Airflow编排网关 */
    private final AirflowOrchestrationGateway airflowGateway;

    public WorkflowServiceImpl(WorkflowRepository workflowRepository,
                               WorkflowNodeRepository workflowNodeRepository,
                               WorkflowEdgeRepository workflowEdgeRepository,
                               WorkflowDependencyRepository workflowDependencyRepository,
                               WorkflowScheduleRepository workflowScheduleRepository,
                               JobRepository jobRepository,
                               AirflowOrchestrationGateway airflowGateway) {
        this.workflowRepository = workflowRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.workflowEdgeRepository = workflowEdgeRepository;
        this.workflowDependencyRepository = workflowDependencyRepository;
        this.workflowScheduleRepository = workflowScheduleRepository;
        this.jobRepository = jobRepository;
        this.airflowGateway = airflowGateway;
    }

    /** 分页查询工作流 */
    @Override
    public Page<WorkflowBO> page(WorkflowPageQuery query) {
        Page<Workflow> page = workflowRepository.page(query);
        List<WorkflowBO> data = Optional.ofNullable(page.getData()).orElse(List.of())
                .stream().map(WorkflowAppConvert.INSTANCE::toWorkflowBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /** 查询工作流详情 */
    @Override
    public WorkflowBO findById(String id) {
        Workflow workflow = workflowRepository.findById(id);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
    }

    /** 保存工作流 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBO save(WorkflowCmd cmd, String createdBy) {
        Workflow workflow = WorkflowAppConvert.INSTANCE.toWorkflow(cmd)
                .setCreatedBy(createdBy)
                .setUpdatedBy(createdBy);
        if (workflow.getWorkflowType() == null) {
            workflow.setWorkflowType(WorkflowType.WORKFLOW);
        }
        workflow = workflow.save(workflowRepository);
        workflow.setDagId(airflowGateway.buildWorkflowDagId(workflow.getId()));
        workflow = workflow.update(workflowRepository);
        return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
    }

    /** 更新工作流 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBO update(String id, WorkflowCmd cmd, String updatedBy) {
        Workflow existing = workflowRepository.findById(id);
        Assert.notNull(existing, new SilentException("工作流不存在"));
        Workflow workflow = WorkflowAppConvert.INSTANCE.toWorkflow(cmd)
                .setId(id)
                .setDagId(existing.getDagId())
                .setStatus(existing.getStatus())
                .setCreatedBy(existing.getCreatedBy())
                .setCreatedAt(existing.getCreatedAt())
                .setUpdatedBy(updatedBy);
        if (workflow.getWorkflowType() == null) {
            workflow.setWorkflowType(existing.getWorkflowType());
        }
        workflow = workflow.update(workflowRepository);
        return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
    }

    /** 删除工作流 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Workflow workflow = workflowRepository.findById(id);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        workflow.delete(workflowRepository);
        workflowScheduleRepository.deleteByWorkflowId(id);
        workflowDependencyRepository.deleteByWorkflowId(id);
        if (workflow.getDagId() != null && !workflow.getDagId().isBlank()) {
            airflowGateway.deleteDag(workflow.getDagId());
        }
    }

    /** 查询工作流定义 */
    @Override
    public WorkflowDefinitionBO findDefinition(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        return new WorkflowDefinitionBO()
                .setWorkflowId(workflowId)
                .setNodes(workflowNodeRepository.listByWorkflowId(workflowId).stream()
                        .map(WorkflowAppConvert.INSTANCE::toNodeBO).toList())
                .setEdges(workflowEdgeRepository.listByWorkflowId(workflowId).stream()
                        .map(WorkflowAppConvert.INSTANCE::toEdgeBO).toList());
    }

    /** 保存工作流定义 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionBO saveDefinition(String workflowId, WorkflowDefinitionCmd cmd, String updatedBy) {
        Workflow workflow = workflowRepository.findById(workflowId);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        List<WorkflowNode> nodes = buildNodes(workflowId, cmd, updatedBy);
        validateNodes(workflow, nodes);
        List<WorkflowNode> savedNodes = workflowNodeRepository.replaceByWorkflowId(workflowId, nodes);
        Map<String, WorkflowNode> nodeByCode = savedNodes.stream()
                .collect(LinkedHashMap::new, (map, node) -> map.put(node.getNodeCode(), node), LinkedHashMap::putAll);
        List<WorkflowEdge> edges = buildEdges(workflowId, cmd, updatedBy, nodeByCode);
        validateAcyclic(savedNodes, edges);
        workflowEdgeRepository.replaceByWorkflowId(workflowId, edges);
        return findDefinition(workflowId);
    }

    /** 查询工作流级依赖 */
    @Override
    public WorkflowDependencyBO findDependencies(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        List<WorkflowBO> upstreamWorkflows = workflowDependencyRepository.listByDownstreamWorkflowId(workflowId).stream()
                .map(WorkflowDependency::getUpstreamWorkflowId)
                .map(workflowRepository::findById)
                .filter(item -> item != null)
                .map(WorkflowAppConvert.INSTANCE::toWorkflowBO)
                .toList();
        List<WorkflowBO> downstreamWorkflows = workflowDependencyRepository.listByUpstreamWorkflowId(workflowId).stream()
                .map(WorkflowDependency::getDownstreamWorkflowId)
                .map(workflowRepository::findById)
                .filter(item -> item != null)
                .map(WorkflowAppConvert.INSTANCE::toWorkflowBO)
                .toList();
        return new WorkflowDependencyBO()
                .setWorkflowId(workflowId)
                .setUpstreamWorkflows(upstreamWorkflows)
                .setDownstreamWorkflows(downstreamWorkflows)
                .setDependencyType(JobDependencyType.SCHEDULE_SAME_CYCLE);
    }

    /** 保存工作流级依赖 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDependencyBO saveDependencies(String workflowId, WorkflowDependencyCmd cmd, String updatedBy) {
        Workflow workflow = workflowRepository.findById(workflowId);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        List<String> upstreamWorkflowIds = Optional.ofNullable(cmd)
                .map(WorkflowDependencyCmd::getUpstreamWorkflowIds)
                .orElse(List.of())
                .stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        List<WorkflowDependency> dependencies = upstreamWorkflowIds.stream()
                .map(upstreamWorkflowId -> buildWorkflowDependency(workflowId, upstreamWorkflowId, updatedBy))
                .toList();
        validateWorkflowDependencyAcyclic(workflowId, dependencies);
        workflowDependencyRepository.replaceByDownstreamWorkflowId(workflowId, dependencies);
        return findDependencies(workflowId);
    }

    /** 查询工作流调度配置 */
    @Override
    public WorkflowScheduleBO findSchedule(String workflowId) {
        WorkflowSchedule schedule = workflowScheduleRepository.findByWorkflowId(workflowId);
        return schedule == null ? null : WorkflowAppConvert.INSTANCE.toScheduleBO(schedule);
    }

    /** 保存工作流调度配置 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowScheduleBO saveSchedule(String workflowId, WorkflowScheduleCmd cmd, String updatedBy) {
        Workflow workflow = workflowRepository.findById(workflowId);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        WorkflowSchedule schedule = new WorkflowSchedule()
                .setWorkflowId(workflowId)
                .setCronExpression(cmd.getCronExpression())
                .setEnabled(cmd.getEnabled())
                .setSchedulerType(cmd.getSchedulerType() == null ? SchedulerType.AIRFLOW : cmd.getSchedulerType())
                .setUpdatedBy(updatedBy);
        validateCronExpressionIfEnabled(schedule);
        WorkflowSchedule existing = workflowScheduleRepository.findByWorkflowId(workflowId);
        WorkflowSchedule result;
        if (existing == null) {
            schedule.setCreatedBy(updatedBy);
            result = schedule.save(workflowScheduleRepository);
        } else {
            schedule.setId(existing.getId()).setCreatedBy(existing.getCreatedBy()).setCreatedAt(existing.getCreatedAt());
            result = schedule.update(workflowScheduleRepository);
        }
        if (workflow.getStatus() == TaskStatus.ONLINE && schedule.getSchedulerType() == SchedulerType.AIRFLOW) {
            airflowGateway.syncDagPaused(workflow.getDagId(), !Boolean.TRUE.equals(schedule.getEnabled()), true);
        }
        return WorkflowAppConvert.INSTANCE.toScheduleBO(result);
    }

    /** 发布工作流 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBO publish(String id, String updatedBy) {
        Workflow workflow = workflowRepository.findById(id);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        validatePublishable(workflow);
        workflow.setUpdatedBy(updatedBy);
        workflow = workflow.publish(workflowRepository);
        WorkflowSchedule schedule = workflowScheduleRepository.findByWorkflowId(id);
        if (schedule != null && schedule.getSchedulerType() == SchedulerType.AIRFLOW) {
            schedule.setEnabled(true);
            workflowScheduleRepository.updateById(schedule);
            airflowGateway.syncDagPaused(workflow.getDagId(), false, false);
        }
        return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
    }

    /** 下线工作流 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBO offline(String id, String updatedBy) {
        Workflow workflow = workflowRepository.findById(id);
        Assert.notNull(workflow, new SilentException("工作流不存在"));
        workflow.setUpdatedBy(updatedBy);
        workflow = workflow.offline(workflowRepository);
        if (workflow.getDagId() != null) {
            airflowGateway.syncDagPaused(workflow.getDagId(), true, false);
        }
        return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
    }

    /** 查询Airflow DAG定义 */
    @Override
    public List<WorkflowDagDefinitionBO> listAirflowDagDefinitions() {
        Map<String, WorkflowSchedule> scheduleByWorkflowId = workflowScheduleRepository.listAirflow().stream()
                .collect(LinkedHashMap::new, (map, schedule) -> map.put(schedule.getWorkflowId(), schedule), LinkedHashMap::putAll);
        return workflowRepository.listAirflowWorkflows().stream()
                .filter(workflow -> workflow.getStatus() == TaskStatus.ONLINE)
                .filter(workflow -> scheduleByWorkflowId.containsKey(workflow.getId()))
                .map(workflow -> buildDagDefinition(workflow, scheduleByWorkflowId.get(workflow.getId())))
                .flatMap(Optional::stream)
                .toList();
    }

    /** 确保作业存在默认单节点工作流 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBO ensureSingleNodeWorkflow(String jobId, String operator) {
        Workflow existing = workflowRepository.findSingleNodeByJobId(jobId);
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        if (existing != null) {
            existing.setName(job.getName())
                    .setDescription(job.getDescription())
                    .setUpdatedBy(operator);
            Workflow workflow = existing.update(workflowRepository);
            WorkflowNode node = new WorkflowNode()
                    .setWorkflowId(workflow.getId())
                    .setJobId(jobId)
                    .setNodeCode("node_" + jobId)
                    .setNodeName(job.getName())
                    .setPositionX(120)
                    .setPositionY(120)
                    .setCreatedBy(workflow.getCreatedBy())
                    .setUpdatedBy(operator);
            node.validateDefinition();
            workflowNodeRepository.replaceByWorkflowId(workflow.getId(), List.of(node));
            return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
        }
        Workflow workflow = new Workflow()
                .setName(job.getName())
                .setDescription(job.getDescription())
                .setWorkflowType(WorkflowType.SINGLE_NODE)
                .setStatus(TaskStatus.DRAFT)
                .setCreatedBy(operator)
                .setUpdatedBy(operator)
                .save(workflowRepository);
        workflow.setDagId(airflowGateway.buildWorkflowDagId(workflow.getId()));
        workflow = workflow.update(workflowRepository);
        WorkflowNode node = new WorkflowNode()
                .setWorkflowId(workflow.getId())
                .setJobId(jobId)
                .setNodeCode("node_" + jobId)
                .setNodeName(job.getName())
                .setPositionX(120)
                .setPositionY(120)
                .setCreatedBy(operator)
                .setUpdatedBy(operator);
        node.validateDefinition();
        workflowNodeRepository.replaceByWorkflowId(workflow.getId(), List.of(node));
        return WorkflowAppConvert.INSTANCE.toWorkflowBO(workflow);
    }

    private List<WorkflowNode> buildNodes(String workflowId, WorkflowDefinitionCmd cmd, String updatedBy) {
        return Optional.ofNullable(cmd).map(WorkflowDefinitionCmd::getNodes).orElse(List.of()).stream()
                .map(nodeCmd -> new WorkflowNode()
                        .setWorkflowId(workflowId)
                        .setJobId(nodeCmd.getJobId())
                        .setNodeCode(firstNotBlank(nodeCmd.getNodeCode(), nodeCmd.getId()))
                        .setNodeName(nodeCmd.getNodeName())
                        .setPositionX(nodeCmd.getPositionX())
                        .setPositionY(nodeCmd.getPositionY())
                        .setConfigJson(nodeCmd.getConfigJson())
                        .setCreatedBy(updatedBy)
                        .setUpdatedBy(updatedBy))
                .peek(WorkflowNode::validateDefinition)
                .toList();
    }

    private void validateNodes(Workflow workflow, List<WorkflowNode> nodes) {
        Assert.isTrue(!nodes.isEmpty(), new SilentException("工作流至少需要一个节点"));
        if (workflow.getWorkflowType() == WorkflowType.SINGLE_NODE) {
            Assert.isTrue(nodes.size() == 1, new SilentException("单节点工作流只能包含一个节点"));
        }
        Set<String> nodeCodes = new LinkedHashSet<>();
        for (WorkflowNode node : nodes) {
            Assert.isTrue(nodeCodes.add(node.getNodeCode()), new SilentException("节点编码重复: " + node.getNodeCode()));
            Job job = jobRepository.findById(node.getJobId());
            Assert.notNull(job, new SilentException("节点关联作业不存在: " + node.getJobId()));
        }
    }

    private List<WorkflowEdge> buildEdges(String workflowId,
                                          WorkflowDefinitionCmd cmd,
                                          String updatedBy,
                                          Map<String, WorkflowNode> nodeByCode) {
        return Optional.ofNullable(cmd).map(WorkflowDefinitionCmd::getEdges).orElse(List.of()).stream()
                .map(edgeCmd -> {
                    String upstreamCode = firstNotBlank(edgeCmd.getUpstreamNodeCode(), edgeCmd.getUpstreamNodeId());
                    String downstreamCode = firstNotBlank(edgeCmd.getDownstreamNodeCode(), edgeCmd.getDownstreamNodeId());
                    WorkflowNode upstreamNode = nodeByCode.get(upstreamCode);
                    WorkflowNode downstreamNode = nodeByCode.get(downstreamCode);
                    Assert.notNull(upstreamNode, new SilentException("上游节点不存在: " + upstreamCode));
                    Assert.notNull(downstreamNode, new SilentException("下游节点不存在: " + downstreamCode));
                    return new WorkflowEdge()
                            .setWorkflowId(workflowId)
                            .setUpstreamNodeId(upstreamNode.getId())
                            .setUpstreamNodeCode(upstreamNode.getNodeCode())
                            .setDownstreamNodeId(downstreamNode.getId())
                            .setDownstreamNodeCode(downstreamNode.getNodeCode())
                            .setDependencyType(Optional.ofNullable(edgeCmd.getDependencyType()).orElse(JobDependencyType.SCHEDULE))
                            .setCreatedBy(updatedBy)
                            .setUpdatedBy(updatedBy);
                })
                .peek(WorkflowEdge::validateDefinition)
                .toList();
    }

    private void validateAcyclic(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Map<String, List<String>> graph = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            graph.computeIfAbsent(edge.getUpstreamNodeId(), key -> new ArrayList<>()).add(edge.getDownstreamNodeId());
        }
        for (WorkflowNode node : nodes) {
            Assert.isTrue(!hasPath(graph, node.getId(), node.getId()), new SilentException("工作流依赖关系成环"));
        }
    }

    private boolean hasPath(Map<String, List<String>> graph, String sourceNodeId, String targetNodeId) {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        queue.add(sourceNodeId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String downstreamNodeId : graph.getOrDefault(current, List.of())) {
                if (targetNodeId.equals(downstreamNodeId)) {
                    return true;
                }
                if (visited.add(downstreamNodeId)) {
                    queue.add(downstreamNodeId);
                }
            }
        }
        return false;
    }

    private void validatePublishable(Workflow workflow) {
        WorkflowSchedule schedule = workflowScheduleRepository.findByWorkflowId(workflow.getId());
        Assert.notNull(schedule, new SilentException("工作流发布前请配置调度"));
        validateCronExpression(schedule.getCronExpression());
        List<WorkflowDependency> workflowDependencies = workflowDependencyRepository.listByDownstreamWorkflowId(workflow.getId());
        validateWorkflowDependencyAcyclic(workflow.getId(), workflowDependencies);
        for (WorkflowDependency dependency : workflowDependencies) {
            Workflow upstream = workflowRepository.findById(dependency.getUpstreamWorkflowId());
            Assert.notNull(upstream, new SilentException("上游工作流不存在: " + dependency.getUpstreamWorkflowId()));
            Assert.isTrue(upstream.getStatus() == TaskStatus.ONLINE, new SilentException("上游工作流未发布: " + upstream.getName()));
            Assert.notBlank(upstream.getDagId(), new SilentException("上游工作流DAG ID为空: " + upstream.getName()));
        }
        List<WorkflowNode> nodes = workflowNodeRepository.listByWorkflowId(workflow.getId());
        Assert.isTrue(!nodes.isEmpty(), new SilentException("工作流发布前请配置节点"));
        List<WorkflowEdge> edges = workflowEdgeRepository.listByWorkflowId(workflow.getId());
        validateAcyclic(nodes, edges);
        for (WorkflowNode node : nodes) {
            Job job = jobRepository.findById(node.getJobId());
            Assert.notNull(job, new SilentException("节点关联作业不存在: " + node.getJobId()));
            Assert.isTrue(job.getStatus() == TaskStatus.ONLINE, new SilentException("节点关联作业未发布: " + job.getName()));
        }
    }

    private Optional<WorkflowDagDefinitionBO> buildDagDefinition(Workflow workflow, WorkflowSchedule schedule) {
        if (!isValidCronExpression(schedule.getCronExpression())) {
            return Optional.empty();
        }
        List<WorkflowNode> nodes = workflowNodeRepository.listByWorkflowId(workflow.getId());
        List<WorkflowEdge> edges = workflowEdgeRepository.listByWorkflowId(workflow.getId());
        List<WorkflowDagDefinitionBO.ExternalDependencyBO> externalDependencies = workflowDependencyRepository.listByDownstreamWorkflowId(workflow.getId()).stream()
                .map(WorkflowDependency::getUpstreamWorkflowId)
                .map(workflowRepository::findById)
                .filter(upstream -> upstream != null && upstream.getStatus() == TaskStatus.ONLINE)
                .map(upstream -> new WorkflowDagDefinitionBO.ExternalDependencyBO()
                        .setUpstreamDagId(upstream.getDagId())
                        .setUpstreamWorkflowId(upstream.getId())
                        .setUpstreamWorkflowName(upstream.getName()))
                .toList();
        Map<String, WorkflowNode> nodeById = nodes.stream()
                .collect(LinkedHashMap::new, (map, node) -> map.put(node.getId(), node), LinkedHashMap::putAll);
        List<WorkflowDagDefinitionBO.TaskBO> tasks = nodes.stream()
                .map(node -> buildDagTask(node, edges, nodeById))
                .flatMap(Optional::stream)
                .toList();
        if (tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new WorkflowDagDefinitionBO()
                .setDagId(workflow.getDagId())
                .setWorkflowId(workflow.getId())
                .setWorkflowName(workflow.getName())
                .setCronExpression(schedule.getCronExpression())
                .setTasks(tasks)
                .setExternalDependencies(externalDependencies));
    }

    private WorkflowDependency buildWorkflowDependency(String downstreamWorkflowId, String upstreamWorkflowId, String updatedBy) {
        Workflow upstream = workflowRepository.findById(upstreamWorkflowId);
        Assert.notNull(upstream, new SilentException("上游工作流不存在: " + upstreamWorkflowId));
        WorkflowDependency dependency = new WorkflowDependency()
                .setUpstreamWorkflowId(upstreamWorkflowId)
                .setDownstreamWorkflowId(downstreamWorkflowId)
                .setDependencyType(JobDependencyType.SCHEDULE_SAME_CYCLE)
                .setCreatedBy(updatedBy)
                .setUpdatedBy(updatedBy);
        dependency.validateDefinition();
        return dependency;
    }

    private void validateWorkflowDependencyAcyclic(String workflowId, List<WorkflowDependency> replacementDependencies) {
        List<WorkflowDependency> dependencies = new ArrayList<>(workflowDependencyRepository.listAll());
        dependencies.removeIf(dependency -> workflowId.equals(dependency.getDownstreamWorkflowId()));
        dependencies.addAll(Optional.ofNullable(replacementDependencies).orElse(List.of()));
        Map<String, List<String>> graph = new HashMap<>();
        for (WorkflowDependency dependency : dependencies) {
            graph.computeIfAbsent(dependency.getUpstreamWorkflowId(), key -> new ArrayList<>()).add(dependency.getDownstreamWorkflowId());
        }
        Assert.isTrue(!hasPath(graph, workflowId, workflowId), new SilentException("工作流级依赖关系成环"));
    }

    private Optional<WorkflowDagDefinitionBO.TaskBO> buildDagTask(WorkflowNode node,
                                                                  List<WorkflowEdge> edges,
                                                                  Map<String, WorkflowNode> nodeById) {
        Job job = jobRepository.findById(node.getJobId());
        if (job == null || job.getStatus() != TaskStatus.ONLINE) {
            return Optional.empty();
        }
        List<String> upstreamTaskIds = edges.stream()
                .filter(edge -> node.getId().equals(edge.getDownstreamNodeId()))
                .map(WorkflowEdge::getUpstreamNodeId)
                .filter(nodeById::containsKey)
                .map(this::taskId)
                .toList();
        return Optional.of(new WorkflowDagDefinitionBO.TaskBO()
                .setTaskId(taskId(node.getId()))
                .setNodeId(node.getId())
                .setJobId(job.getId())
                .setJobName(job.getName())
                .setEngineType(job.getEngineType())
                .setNodeType(job.getNodeType())
                .setUpstreamTaskIds(upstreamTaskIds));
    }

    private String taskId(String nodeId) {
        return "node_" + nodeId;
    }

    private void validateCronExpressionIfEnabled(WorkflowSchedule schedule) {
        if (Boolean.TRUE.equals(schedule.getEnabled())) {
            validateCronExpression(schedule.getCronExpression());
        }
    }

    private void validateCronExpression(String cronExpression) {
        Assert.isTrue(isValidCronExpression(cronExpression), new SilentException("Cron表达式不合法，请检查格式: " + cronExpression));
    }

    private boolean isValidCronExpression(String cronExpression) {
        String normalizedCron = normalizeToAirflowCron(cronExpression);
        if (normalizedCron == null || normalizedCron.isBlank()) {
            return false;
        }
        try {
            CronExpression.parse("0 " + normalizedCron);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String normalizeToAirflowCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return null;
        }
        List<String> rawParts = Arrays.stream(cronExpression.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .toList();
        if (rawParts.isEmpty()) {
            return null;
        }
        List<String> parts = rawParts.stream()
                .map(part -> part.replace("?", "*").replace("？", "*"))
                .toList();
        if (parts.size() == 5) {
            if (rawParts.get(4).endsWith("?") || rawParts.get(4).endsWith("？")) {
                return String.join(" ", parts.get(1), parts.get(2), parts.get(3), "*", "*");
            }
            return String.join(" ", parts);
        }
        if (parts.size() == 6 || parts.size() == 7) {
            return String.join(" ", parts.subList(1, 6));
        }
        return null;
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
