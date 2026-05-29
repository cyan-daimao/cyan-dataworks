package com.cyan.dataworks.application.job.dependency.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.application.job.bo.JobBO;
import com.cyan.dataworks.application.job.convert.JobAppConvert;
import com.cyan.dataworks.application.job.dependency.JobDependencyService;
import com.cyan.dataworks.application.job.dependency.bo.JobDependencyBO;
import com.cyan.dataworks.application.job.dependency.bo.JobLineageBO;
import com.cyan.dataworks.application.job.dependency.cmd.JobDependencyCmd;
import com.cyan.dataworks.application.job.dependency.convert.JobDependencyAppConvert;
import com.cyan.dataworks.application.job.lineage.JobLineageSyncService;
import com.cyan.dataworks.domain.job.Job;
import com.cyan.dataworks.domain.job.dependency.JobDependency;
import com.cyan.dataworks.domain.job.dependency.repository.JobDependencyRepository;
import com.cyan.dataworks.domain.job.repository.JobRepository;
import com.cyan.dataworks.enums.JobDependencyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作业依赖应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class JobDependencyServiceImpl implements JobDependencyService {

    /**
     * 作业仓储
     */
    private final JobRepository jobRepository;

    /**
     * 作业依赖仓储
     */
    private final JobDependencyRepository jobDependencyRepository;

    /**
     * 作业血缘同步服务
     */
    private final JobLineageSyncService jobLineageSyncService;

    public JobDependencyServiceImpl(JobRepository jobRepository,
                                    JobDependencyRepository jobDependencyRepository,
                                    JobLineageSyncService jobLineageSyncService) {
        this.jobRepository = jobRepository;
        this.jobDependencyRepository = jobDependencyRepository;
        this.jobLineageSyncService = jobLineageSyncService;
    }

    /**
     * 查询作业依赖
     */
    @Override
    public JobDependencyBO findByJobId(String jobId) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        List<JobBO> upstreamJobs = jobDependencyRepository.listByDownstreamJobId(jobId).stream()
                .map(JobDependency::getUpstreamJobId)
                .map(jobRepository::findById)
                .filter(upstream -> upstream != null)
                .map(JobAppConvert.INSTANCE::toJobBO)
                .toList();
        List<JobBO> downstreamJobs = jobDependencyRepository.listByUpstreamJobId(jobId).stream()
                .map(JobDependency::getDownstreamJobId)
                .map(jobRepository::findById)
                .filter(downstream -> downstream != null)
                .map(JobAppConvert.INSTANCE::toJobBO)
                .toList();
        return new JobDependencyBO()
                .setJobId(jobId)
                .setUpstreamJobs(upstreamJobs)
                .setDownstreamJobs(downstreamJobs);
    }

    /**
     * 保存作业依赖
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobDependencyBO save(String jobId, JobDependencyCmd cmd, String updatedBy) {
        Job downstreamJob = jobRepository.findById(jobId);
        Assert.notNull(downstreamJob, new SilentException("作业不存在"));
        List<String> upstreamJobIds = normalizeUpstreamJobIds(cmd);
        validateUpstreamJobs(jobId, upstreamJobIds);
        validateAcyclic(jobId, upstreamJobIds);
        List<JobDependency> dependencies = upstreamJobIds.stream()
                .map(upstreamJobId -> new JobDependency()
                        .setUpstreamJobId(upstreamJobId)
                        .setDownstreamJobId(jobId)
                        .setDependencyType(JobDependencyType.SCHEDULE)
                        .setCreatedBy(updatedBy)
                        .setUpdatedBy(updatedBy))
                .toList();
        dependencies.forEach(JobDependency::validateDefinition);
        jobDependencyRepository.replaceByDownstreamJobId(jobId, dependencies);
        List<Job> upstreamJobs = upstreamJobIds.stream()
                .map(jobRepository::findById)
                .filter(job -> job != null)
                .toList();
        jobLineageSyncService.syncDependencies(downstreamJob, upstreamJobs);
        return findByJobId(jobId);
    }

    /**
     * 查询作业血缘
     */
    @Override
    public JobLineageBO lineage(String jobId) {
        Job job = jobRepository.findById(jobId);
        Assert.notNull(job, new SilentException("作业不存在"));
        List<JobDependency> dependencies = jobDependencyRepository.listAll();
        Set<String> relatedJobIds = collectRelatedJobIds(jobId, dependencies);
        Map<String, JobLineageBO.NodeBO> nodes = new LinkedHashMap<>();
        for (String relatedJobId : relatedJobIds) {
            Job relatedJob = jobRepository.findById(relatedJobId);
            if (relatedJob != null) {
                nodes.put(relatedJobId, JobDependencyAppConvert.INSTANCE.toNodeBO(relatedJob));
            }
        }
        List<JobLineageBO.EdgeBO> edges = dependencies.stream()
                .filter(dependency -> relatedJobIds.contains(dependency.getUpstreamJobId())
                        && relatedJobIds.contains(dependency.getDownstreamJobId()))
                .map(dependency -> new JobLineageBO.EdgeBO()
                        .setUpstreamJobId(dependency.getUpstreamJobId())
                        .setDownstreamJobId(dependency.getDownstreamJobId())
                        .setDependencyType(dependency.getDependencyType()))
                .toList();
        return new JobLineageBO()
                .setJobId(jobId)
                .setNodes(new ArrayList<>(nodes.values()))
                .setEdges(edges);
    }

    /**
     * 删除作业相关依赖
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByJobId(String jobId) {
        List<String> affectedDownstreamJobIds = jobDependencyRepository.listByUpstreamJobId(jobId)
                .stream()
                .map(JobDependency::getDownstreamJobId)
                .collect(Collectors.toCollection(ArrayList::new));
        affectedDownstreamJobIds.add(jobId);
        jobDependencyRepository.deleteByJobId(jobId);
        affectedDownstreamJobIds.stream().distinct().forEach(downstreamJobId -> {
            Job downstreamJob = jobRepository.findById(downstreamJobId);
            if (downstreamJob == null) {
                return;
            }
            List<Job> upstreamJobs = jobDependencyRepository.listByDownstreamJobId(downstreamJobId).stream()
                    .map(JobDependency::getUpstreamJobId)
                    .map(jobRepository::findById)
                    .filter(upstreamJob -> upstreamJob != null)
                    .toList();
            jobLineageSyncService.syncDependencies(downstreamJob, upstreamJobs);
        });
    }

    /**
     * 归一化上游作业ID
     */
    private List<String> normalizeUpstreamJobIds(JobDependencyCmd cmd) {
        return Optional.ofNullable(cmd)
                .map(JobDependencyCmd::getUpstreamJobIds)
                .orElse(List.of())
                .stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    /**
     * 校验上游作业存在
     */
    private void validateUpstreamJobs(String downstreamJobId, List<String> upstreamJobIds) {
        for (String upstreamJobId : upstreamJobIds) {
            Assert.isTrue(!downstreamJobId.equals(upstreamJobId), new SilentException("作业不能依赖自身"));
            Job upstreamJob = jobRepository.findById(upstreamJobId);
            Assert.notNull(upstreamJob, new SilentException("上游作业不存在: " + upstreamJobId));
        }
    }

    /**
     * 校验依赖关系无环
     */
    private void validateAcyclic(String downstreamJobId, List<String> upstreamJobIds) {
        Map<String, List<String>> graph = jobDependencyRepository.listAll().stream()
                .filter(dependency -> !downstreamJobId.equals(dependency.getDownstreamJobId()))
                .collect(Collectors.groupingBy(
                        JobDependency::getUpstreamJobId,
                        LinkedHashMap::new,
                        Collectors.mapping(JobDependency::getDownstreamJobId, Collectors.toList())
                ));
        for (String upstreamJobId : upstreamJobIds) {
            graph.computeIfAbsent(upstreamJobId, key -> new ArrayList<>()).add(downstreamJobId);
        }
        for (String upstreamJobId : upstreamJobIds) {
            Assert.isTrue(!hasPath(graph, downstreamJobId, upstreamJobId),
                    new SilentException("依赖关系成环，请调整上游任务"));
        }
    }

    /**
     * 判断图中是否存在路径
     */
    private boolean hasPath(Map<String, List<String>> graph, String sourceJobId, String targetJobId) {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        queue.add(sourceJobId);
        visited.add(sourceJobId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String downstreamJobId : graph.getOrDefault(current, List.of())) {
                if (targetJobId.equals(downstreamJobId)) {
                    return true;
                }
                if (visited.add(downstreamJobId)) {
                    queue.add(downstreamJobId);
                }
            }
        }
        return false;
    }

    /**
     * 收集当前作业相关节点
     */
    private Set<String> collectRelatedJobIds(String jobId, List<JobDependency> dependencies) {
        Set<String> result = new LinkedHashSet<>();
        result.add(jobId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (JobDependency dependency : dependencies) {
                boolean upstreamKnown = result.contains(dependency.getUpstreamJobId());
                boolean downstreamKnown = result.contains(dependency.getDownstreamJobId());
                if (upstreamKnown && result.add(dependency.getDownstreamJobId())) {
                    changed = true;
                }
                if (downstreamKnown && result.add(dependency.getUpstreamJobId())) {
                    changed = true;
                }
            }
        }
        return result;
    }
}
