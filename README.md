# cyan-dataworks

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.7-orange.svg)](https://baomidou.com/)
[![MapStruct](https://img.shields.io/badge/MapStruct-1.5.5.Final-blueviolet.svg)](https://mapstruct.org/)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)]()

`cyan-dataworks` 是 Cyan 数据平台的数据加工、任务调度和工作流编排服务。当前版本已经完成单节点任务 `Job` 与工作流 `Workflow` 的解耦：

- `data_work_job` 只表示单节点任务。
- `data_work_workflow` 只表示真正的工作流。
- 工作流节点是 workflow 私有节点，节点自身保存 `engineType`、`nodeType`、`content`、`configJson`，不再引用 `job_id`。
- Airflow DAG 分为两类动态脚本：单节点 Job DAG 和 Workflow DAG。

---

## 模块说明

| 模块 | artifactId | 说明 |
| --- | --- | --- |
| 根项目 | `cyan-dataworks` | 父 POM，统一依赖版本、构建插件和模块声明 |
| 应用服务 | `cyan-dataworks-application` | Spring Boot 应用，包含 HTTP/RPC Controller、应用服务、领域模型、仓储实现、Airflow/Kubernetes/RustFS/Flink 集成 |
| 客户端契约 | `cyan-dataworks-client` | 对外 Feign client、DTO、request、query、enum，供 Airflow 动态脚本回调、其他服务 RPC 调用 |
| Spark Runner | `cyan-dataworks-spark-runner` | Spark Operator 的 Java Application 入口，读取 SQL 文件并通过 `SparkSession` 执行 |

---

## 架构分层

服务遵循 DDBD / DDD 四层结构：

```text
adapter -> application -> domain -> infra
```

| 分层 | 主要职责 | 当前目录 |
| --- | --- | --- |
| `adapter` | HTTP/RPC 入口、DTO/Cmd 转换、用户上下文读取 | `adapter/job`、`adapter/job_instance`、`adapter/workflow`、`adapter/task` |
| `application` | 用例编排、事务控制、跨领域协调、Airflow/K8s 调用编排 | `application/job`、`application/job_instance`、`application/workflow` |
| `domain` | 充血领域模型、状态流转、业务校验、仓储接口 | `domain/job`、`domain/job_instance`、`domain/workflow` |
| `infra` | MyBatis-Plus DO/Mapper/Repository、远程服务、配置 | `infra/persistence`、`infra/remote`、`infra/config` |

读写对象链路：

```text
读：DO -> Domain -> BO -> DTO
写：Cmd -> Domain -> Repository -> DO
```

---

## 核心领域模型

### Job：单节点任务

`Job` 是一个可独立编辑、发布、调度和运行的单节点任务。

主要能力：

- 任务 CRUD。
- 支持 `SPARK_SQL`、`FLINK_SQL`、`SPARK_BATCH`、`FLINK_BATCH`、`SHELL`、`PYTHON` 等节点类型。
- 发布 / 下线。
- 单节点调度配置，写入 `data_work_job_schedule`。
- DAG 级依赖，写入 `data_work_job_dependency`。
- 任务实例、日志、重试、终止。
- SQL 字段血缘提取与依赖血缘同步。

### Workflow：真正工作流

`Workflow` 是多节点 DAG 编排实体，不再和 `Job` 混表或镜像。

主要能力：

- 工作流 CRUD。
- 工作流画布定义：`nodes + edges`。
- 工作流节点自包含执行信息，不引用 `Job`。
- 工作流内部依赖：`data_work_workflow_edge`。
- DAG 级上游工作流依赖：`data_work_workflow_dependency`。
- 工作流调度配置：`data_work_workflow_schedule`。
- 发布 / 下线。
- DAG Run 触发、历史查询、Task Instance 查询、重跑、标记状态。

### JobInstance：执行实例

`JobInstance` 记录单节点 Job 或 Workflow Node 的一次运行。

主要能力：

- 记录运行状态、执行耗时、错误信息、结果摘要。
- 支持 `workflow_instance_id`、`workflow_node_id` 关联工作流运行。
- `job_id` 允许为空，用于工作流私有节点实例。
- Shell/Python 支持 Pod 异步回调字段：`runtimeJobName`、`logObjectKey`、`startedAt`、`finishedAt`、`callbackAt`。
- 日志优先从 RustFS `logObjectKey` 读取，`resultData/errorMessage` 作为摘要和兜底。

---

## 执行与调度链路

### 单节点 Job DAG

Airflow 动态脚本：`deploy/airflow/dags/dataworks_job_dag_factory.py`

链路：

```text
Airflow Scheduler
  -> /rpc/dataworks/airflow/job-dag-definitions
  -> 注册 dataworks_job_{jobId}
  -> HttpOperator submit: /rpc/dataworks/job-instances/{jobId}/run-by-scheduler
  -> PythonSensor wait: /rpc/dataworks/job-instances/{instanceId}/scheduler-status
  -> JobInstance 终态决定 Airflow task 成功或失败
```

### Workflow DAG

Airflow 动态脚本：`deploy/airflow/dags/dataworks_workflow_dag_factory.py`

链路：

```text
Airflow Scheduler
  -> /rpc/dataworks/airflow/workflow-dag-definitions
  -> 注册 dataworks_workflow_{workflowId}
  -> 每个 workflow node 生成 submit + wait TaskGroup
  -> workflow edge 转成 DAG 内 task 依赖
  -> workflow dependency 转成 ExternalTaskSensor
```

### Shell/Python Pod 回调

Shell/Python 任务不再依赖 `run-by-scheduler` 的 HTTP 长连接等待 Pod 结束。

```text
run-by-scheduler
  -> 创建 JobInstance(RUNNING)
  -> 异步提交 Kubernetes Job
  -> 立即返回 instanceId

Kubernetes Pod
  -> 执行脚本
  -> 上传日志到 RustFS
  -> 回调 /rpc/dataworks/job-instances/{instanceId}/callback

Airflow Sensor
  -> 轮询 scheduler-status
  -> SUCCESS 结束成功
  -> FAILED 抛错失败
```

Flink 任务当前仍保留现有 Flink/Kubernetes Operator 相关链路。

### SparkSQL 执行链路

当前 `SPARK_SQL` 不再通过 `cyan-datagateway` 执行。调度触发后，DataWorks 创建本地 `JobInstance`，再由 `SparkSqlJobExecutor` 通过 KubernetesClient 创建 `SparkApplication`，由 Spark Operator 创建 driver/executor Pod。

```text
Airflow
  -> /rpc/dataworks/job-instances/{jobId}/run-by-scheduler
  -> JobInstanceServiceImpl.executeJob
  -> JobExecutorRegistry.get(SPARK_SQL)
  -> SparkSqlJobExecutor
  -> SparkApplicationOperatorService
  -> ConfigMap(job.sql) + SparkApplication
  -> Spark Operator
  -> Spark driver/executor Pod
  -> scheduler-status 同步 SparkApplication 终态
```

Spark SQL 代码会写入 ConfigMap 并挂载到 driver Pod，`cyan-dataworks-spark-runner` 的 `com.cyan.dataworks.spark.SqlRunner` 读取 SQL 文件后通过 `SparkSession` 执行。Airflow task 仍通过 `PythonSensor` 等待 `JobInstance` 终态。

`SPARK_BATCH` 当前仍是占位实现，`UnsupportedBatchJobExecutor` 会提示批任务提交执行器尚未配置；后续可扩展为提交用户指定 jar/python 的 `SparkApplication`。

---

## 对外 HTTP API

统一前缀：`/api/v1/data-work`

### Job API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/jobs` | 分页查询单节点任务 |
| `GET` | `/jobs/{id}` | 查询任务详情 |
| `POST` | `/jobs` | 创建任务 |
| `PUT` | `/jobs/{id}` | 更新任务 |
| `DELETE` | `/jobs/{id}` | 删除任务 |
| `PUT` | `/jobs/{id}/publish` | 发布任务 |
| `PUT` | `/jobs/{id}/offline` | 下线任务 |
| `GET` | `/jobs/{jobId}/schedule` | 查询任务调度配置 |
| `PUT` | `/jobs/{jobId}/schedule` | 保存任务调度配置 |
| `GET` | `/jobs/{jobId}/dependencies` | 查询任务上下游依赖 |
| `PUT` | `/jobs/{jobId}/dependencies` | 保存任务上游依赖 |
| `GET` | `/jobs/{jobId}/lineage` | 查询任务依赖血缘图 |

### Job Instance API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/jobs/{jobId}/execute` | 手动执行已保存任务 |
| `POST` | `/jobs/execute-preview` | 临时执行任务内容，不要求先保存 |
| `POST` | `/jobs/{jobId}/start` | 启动作业应用 |
| `GET` | `/jobs/{jobId}/instances` | 查询某个任务实例列表 |
| `GET` | `/instances` | 查询全部实例 |
| `GET` | `/instances/{id}` | 查询实例详情 |
| `GET` | `/instances/{id}/logs` | 查询实例日志 |
| `POST` | `/instances/{id}/retry` | 重试实例 |
| `POST` | `/instances/{id}/terminate` | 终止实例 |

### Workflow API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/workflows` | 分页查询工作流 |
| `GET` | `/workflows/{id}` | 查询工作流详情 |
| `POST` | `/workflows` | 创建工作流 |
| `PUT` | `/workflows/{id}` | 更新工作流基础信息 |
| `DELETE` | `/workflows/{id}` | 删除工作流 |
| `GET` | `/workflows/{id}/definition` | 查询工作流节点和连线定义 |
| `PUT` | `/workflows/{id}/definition` | 保存工作流节点和连线定义 |
| `GET` | `/workflows/{id}/dependencies` | 查询 DAG 级上下游工作流依赖 |
| `PUT` | `/workflows/{id}/dependencies` | 保存 DAG 级上游工作流依赖 |
| `GET` | `/workflows/{id}/schedule` | 查询工作流调度配置 |
| `PUT` | `/workflows/{id}/schedule` | 保存工作流调度配置 |
| `PUT` | `/workflows/{id}/publish` | 发布工作流 |
| `PUT` | `/workflows/{id}/offline` | 下线工作流 |

### Workflow Run / Airflow API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/workflows/{id}/dag-runs` | 触发 DAG Run |
| `GET` | `/workflows/{id}/instances` | 查询工作流运行实例 |
| `GET` | `/workflows/{id}/dag-runs` | 查询 Airflow DAG Run 列表 |
| `GET` | `/workflows/{id}/dag-runs/{runId}` | 查询 DAG Run 详情 |
| `PATCH` | `/workflows/{id}/dag-runs/{runId}/state` | 修改 DAG Run 状态 |
| `GET` | `/workflows/{id}/dag-runs/{runId}/task-instances` | 查询 Task Instance 列表 |
| `POST` | `/workflows/{id}/dag-runs/{runId}/task-instances/{taskId}/rerun` | 清理并重跑 Task Instance |
| `PATCH` | `/workflows/{id}/dag-runs/{runId}/task-instances/{taskId}/state` | 标记 Task Instance 状态 |

### Folder API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/folders` | 查询任务目录 |
| `POST` | `/folders` | 创建目录 |
| `PUT` | `/folders/{id}` | 更新目录 |
| `DELETE` | `/folders/{id}` | 删除目录 |

---

## RPC API

RPC 接口供 Airflow、Pod runner、其他服务内部调用，统一使用 `/rpc/dataworks` 前缀。

| 方法 | 路径 | 调用方 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/airflow/job-dag-definitions` | Airflow DAG Factory | 获取单节点 Job DAG 定义 |
| `GET` | `/airflow/workflow-dag-definitions` | Airflow DAG Factory | 获取 Workflow DAG 定义 |
| `POST` | `/job-instances/{jobId}/run-by-scheduler` | Airflow HttpOperator | 调度触发单节点 Job，返回 `instanceId` |
| `GET` | `/job-instances/{instanceId}/scheduler-status` | Airflow Sensor | 查询实例是否到达终态 |
| `POST` | `/job-instances/{instanceId}/callback` | Kubernetes Pod runner | Pod 结束后回调执行结果和日志对象 |
| `POST` | `/workflows/{workflowId}/nodes/{nodeId}/run-by-scheduler` | Airflow HttpOperator | 调度触发 Workflow Node |
| `GET` | `/job-instances/{instanceId}` | 内部调用 | 查询实例详情 |
| `POST` | `/jobs` | 内部调用 | 创建 Job |
| `POST` | `/jobs/{id}/publish` | 内部调用 | 发布 Job |

---

## Airflow 集成

### REST API

`infra.remote.airflow` 封装 Airflow v1 REST API：

- `AirflowDagApiClient`：DAG 查询、启停、删除。
- `AirflowDagRunApiClient`：触发 DAG Run、查询历史、修改状态。
- `AirflowTaskInstanceApiClient`：查询 Task Instance。
- `AirflowTaskClearApiClient`：清理 Task Instance 以实现重跑。
- `AirflowOrchestrationGateway`：统一鉴权、错误转换和 DTO 转换。

支持 Basic Auth 和 Bearer Token。Airflow 侧需要启用 Basic Auth 时，应在 Helm values 中配置：

```yaml
config:
  api:
    auth_backends: "airflow.api.auth.backend.basic_auth,airflow.api.auth.backend.session"
```

### 动态 DAG 脚本

| 文件 | 说明 |
| --- | --- |
| `deploy/airflow/dags/dataworks_job_dag_factory.py` | 单节点 Job DAG 注册脚本 |
| `deploy/airflow/dags/dataworks_workflow_dag_factory.py` | Workflow DAG 注册脚本 |
| `deploy/airflow/dags/dataworks_dynamic_dag_factory.py` | 旧版动态脚本，保留用于历史参考 |
| `deploy/airflow/values.yaml` | Airflow Helm values 示例 |

---

## Kubernetes / RustFS / Flink 集成

### Kubernetes

`infra.remote.kubernetes.ScriptKubernetesJobService` 用于提交 Shell/Python Kubernetes Job。

关键配置前缀：`dataworks.script-runtime`

| 配置 | 说明 |
| --- | --- |
| `namespace` | 运行脚本 Pod 的 Kubernetes namespace |
| `shellImage` / `pythonImage` | Shell/Python runner 镜像 |
| `cpu` / `memory` | 默认资源限制 |
| `timeoutSeconds` | 默认执行超时 |
| `ttlSecondsAfterFinished` | Job 完成后保留时间 |
| `callbackBaseUrl` | Pod 回调 DataWorks 的服务地址 |
| `callbackToken` | Pod 回调鉴权 Token |
| `rustfsEndpoint` / `rustfsAccessKey` / `rustfsSecretKey` | RustFS S3-compatible 访问配置 |
| `logBucket` / `logBasePrefix` | 脚本日志存储位置 |

### RustFS 日志

`infra.remote.rustfs.RustFsLogService` 负责读取脚本日志对象。RustFS 使用 S3-compatible SDK 访问，但业务文案统一称 RustFS。

日志读取优先级：

1. `JobInstance.logObjectKey` 对应的 RustFS 日志文件。
2. `resultData`。
3. `errorMessage`。

### Flink

`infra.remote.flink` 提供 Flink SQL / Flink Application 相关能力：

- `FlinkRemoteService`
- `FlinkRpcClient`
- `FlinkApplicationOperatorService`
- Flink Pod 日志查询。

RBAC 示例文件：

```text
deploy/kubernetes/dataworks-flink-pod-log-rbac.yaml
```

### Spark

`infra.remote.spark.operator.SparkApplicationOperatorService` 负责提交和查询 Spark Operator `SparkApplication`。

关键配置前缀：`dataworks.spark-operator`

| 配置 | 说明 |
| --- | --- |
| `namespace` | SparkApplication 所在 Kubernetes namespace |
| `image` | 内置 `cyan-dataworks-spark-runner` 的 Spark 镜像 |
| `mainApplicationFile` / `mainClass` | Spark Runner jar 和入口类 |
| `serviceAccount` | Spark driver/executor 使用的 service account |
| `driverCores` / `driverMemory` | Driver 默认资源 |
| `executorInstances` / `executorCores` / `executorMemory` | Executor 默认资源 |
| `icebergRestUri` | Iceberg REST Catalog 地址 |
| `rustfsEndpoint` / `rustfsAccessKey` / `rustfsSecretKey` | RustFS S3-compatible 访问配置 |

RBAC 需要允许 DataWorks 操作 `sparkoperator.k8s.io/sparkapplications`、`configmaps`、`pods` 和 `pods/log`。

---

## 数据库变更脚本

| 文件 | 说明 |
| --- | --- |
| `deploy/sql/20260529_pod_callback_workflow_dependency.sql` | 增加 Pod 回调字段和 `data_work_workflow_dependency` |
| `deploy/sql/20260529_job_workflow_decouple.sql` | Job / Workflow 解耦迁移，清理旧 `SINGLE_NODE workflow`，工作流节点增加自包含字段 |

关键表：

| 表 | 说明 |
| --- | --- |
| `data_work_job` | 单节点任务定义 |
| `data_work_job_schedule` | 单节点任务调度配置 |
| `data_work_job_dependency` | 单节点 Job DAG 级依赖 |
| `data_work_job_instance` | Job / Workflow Node 执行实例 |
| `data_work_workflow` | 真正工作流定义 |
| `data_work_workflow_node` | 工作流私有节点 |
| `data_work_workflow_edge` | 工作流内部节点依赖 |
| `data_work_workflow_schedule` | 工作流调度配置 |
| `data_work_workflow_dependency` | 工作流 DAG 级依赖 |
| `data_work_workflow_instance` | 工作流运行实例 |

---

## 血缘能力

当前血缘能力分两类：

- SQL 字段血缘：`SqlFieldLineageExtractor` 从 SQL 内容中解析字段级输入输出关系。
- 调度依赖血缘：Job / Workflow 依赖关系同步为 `ETL_JOB -> ETL_JOB` 或 workflow 级依赖边。

同步入口：

- `application.job.lineage.JobLineageSyncService`
- `infra.gateway.MetadataLineageGateway`

---

## 构建与运行

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Kubernetes 集群，用于 Shell/Python Pod 和 Flink Application。
- Airflow 2.x，当前联调版本为 2.11.x。
- RustFS，提供 S3-compatible 日志存储。

### 构建

```bash
mvn clean package -DskipTests
```

推荐按模块构建：

```bash
mvn -pl cyan-dataworks-application -am package -DskipTests
```

### 本地运行

```bash
cd cyan-dataworks-application
mvn spring-boot:run
```

### 客户端依赖

```xml
<dependency>
    <groupId>com.cyan</groupId>
    <artifactId>cyan-dataworks-client</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

---

## 开发约定

- 普通前端接口使用 `/api/v1/data-work/**`。
- Airflow、Pod runner、内部服务回调使用 `/rpc/dataworks/**`。
- 新增 RPC 契约优先放在 `cyan-dataworks-client`，实现放在 `cyan-dataworks-application` 的 `adapter/**/rpc`。
- Controller 不直接访问 Mapper、Repository 或 DO。
- Application 层负责用例编排，Domain 层负责业务状态变化和规则校验。
- 所有新增表必须包含 `created_at`、`updated_at`、`deleted_at`，推荐包含 `created_by`、`updated_by`。
- 软删唯一索引需要把 `deleted_at` 纳入联合唯一索引，避免历史软删数据阻塞重建。

---

## 当前边界

- Job 与 Workflow 已解耦，不再创建 `SINGLE_NODE workflow`。
- Workflow 节点不复用 `data_work_job`。
- SparkSQL 当前通过 Spark Operator 创建 `SparkApplication` 执行，不再走 `cyan-datagateway`。
- Spark Batch 提交能力尚未接入。
- Flink 执行链路暂未完全切换到 Pod callback + Sensor 模型。
- Airflow DAG 文件仍由动态 Python 脚本注册，不通过 REST API 直接创建 DAG 文件。
- 生产环境应使用专用 Airflow 用户或 Token，并收敛最小权限；测试环境可能仍使用 `admin/admin`。
