CREATE TABLE IF NOT EXISTS task_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件夹ID，主键，自增',
    name VARCHAR(200) NOT NULL COMMENT '文件夹名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_parent_id (parent_id) COMMENT '父文件夹索引，用于树形查询'
) COMMENT = '任务文件夹表，用于对数据加工任务进行分组组织';

CREATE TABLE IF NOT EXISTS data_work_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业ID，主键，自增',
    folder_id BIGINT DEFAULT 0 COMMENT '所属文件夹ID',
    name VARCHAR(200) NOT NULL COMMENT '作业名称',
    description VARCHAR(500) COMMENT '作业描述',
    engine_type VARCHAR(20) NOT NULL COMMENT '引擎类型：SPARK / FLINK / SHELL / PYTHON',
    node_type VARCHAR(30) DEFAULT 'SPARK_SQL' COMMENT '节点类型：SPARK_SQL / FLINK_SQL / SPARK_BATCH / FLINK_BATCH / SHELL / PYTHON / DATA_QUALITY / VIRTUAL',
    content TEXT NOT NULL COMMENT '任务内容，支持SQL、Shell、Python等正文',
    config_json TEXT COMMENT '节点配置JSON',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '作业状态：DRAFT（草稿）/ ONLINE（已上线）/ OFFLINE（已下线）',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_folder_id (folder_id) COMMENT '文件夹索引',
    INDEX idx_name (name) COMMENT '作业名称索引，用于模糊搜索',
    INDEX idx_engine_type (engine_type) COMMENT '引擎类型索引，用于筛选'
) COMMENT = '数据加工作业表（新版），对标Spark/Flink的Job概念，存储作业定义';

CREATE TABLE IF NOT EXISTS data_work_job_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实例ID，主键，自增',
    job_id BIGINT NOT NULL COMMENT '关联的作业ID',
    workflow_instance_id BIGINT COMMENT '关联的工作流实例ID',
    workflow_node_id BIGINT COMMENT '关联的工作流节点ID',
    job_name VARCHAR(200) COMMENT '作业名称（快照，防止作业改名后丢失历史名称）',
    engine_type VARCHAR(20) NOT NULL COMMENT '引擎类型：SPARK / FLINK / SHELL / PYTHON',
    content TEXT NOT NULL COMMENT '任务内容（快照，记录执行时的SQL、Shell、Python等正文）',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：RUNNING（运行中）/ SUCCESS（成功）/ FAILED（失败）',
    cost_time_ms BIGINT COMMENT '执行耗时，单位毫秒',
    result_data TEXT COMMENT '结果数据，JSON格式存储',
    error_message TEXT COMMENT '错误信息，执行失败时记录',
    application_name VARCHAR(200) COMMENT 'Flink Application名称',
    application_namespace VARCHAR(100) COMMENT 'Flink Application命名空间',
    config_map_name VARCHAR(200) COMMENT 'Flink ConfigMap名称',
    job_manager_pod_name VARCHAR(200) COMMENT 'JobManager Pod名称',
    task_manager_pod_names TEXT COMMENT 'TaskManager Pod名称列表，JSON格式存储',
    scheduler_type VARCHAR(20) COMMENT '调度器类型：INTERNAL / AIRFLOW',
    scheduler_dag_id VARCHAR(200) COMMENT '调度器DAG ID',
    scheduler_dag_run_id VARCHAR(200) COMMENT '调度器DAG运行ID',
    scheduler_task_id VARCHAR(200) COMMENT '调度器任务ID',
    scheduler_try_number INT COMMENT '调度器重试次数',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，即执行开始时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_job_id (job_id) COMMENT '作业ID索引，用于按作业查询实例',
    INDEX idx_workflow_instance_id (workflow_instance_id) COMMENT '工作流实例ID索引',
    INDEX idx_workflow_node_id (workflow_node_id) COMMENT '工作流节点ID索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引，用于时间范围查询'
) COMMENT = '作业实例表（新版），对标Spark的Task实例概念，存储每次执行的运行快照';

CREATE TABLE IF NOT EXISTS data_work_workflow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工作流ID',
    name VARCHAR(200) NOT NULL COMMENT '工作流名称',
    description VARCHAR(500) COMMENT '工作流描述',
    workflow_type VARCHAR(30) NOT NULL DEFAULT 'WORKFLOW' COMMENT '工作流类型：SINGLE_NODE / WORKFLOW',
    dag_id VARCHAR(200) COMMENT 'Airflow DAG ID',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '工作流状态：DRAFT / ONLINE / OFFLINE',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_name (name) COMMENT '工作流名称索引',
    INDEX idx_dag_id (dag_id) COMMENT 'DAG ID索引',
    INDEX idx_workflow_type (workflow_type) COMMENT '工作流类型索引'
) COMMENT = '数据加工工作流表，所有Airflow DAG均由工作流生成';

CREATE TABLE IF NOT EXISTS data_work_workflow_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工作流节点ID',
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    job_id BIGINT NOT NULL COMMENT '关联作业ID',
    node_code VARCHAR(100) NOT NULL COMMENT '节点编码，工作流内稳定唯一',
    node_name VARCHAR(200) NOT NULL COMMENT '节点名称',
    position_x INT DEFAULT 0 COMMENT '画布X坐标',
    position_y INT DEFAULT 0 COMMENT '画布Y坐标',
    config_json TEXT COMMENT '节点配置JSON',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    UNIQUE KEY uk_workflow_node_code (workflow_id, node_code, deleted_at) COMMENT '工作流内节点编码唯一',
    INDEX idx_workflow_id (workflow_id) COMMENT '工作流ID索引',
    INDEX idx_job_id (job_id) COMMENT '作业ID索引'
) COMMENT = '工作流节点表';

CREATE TABLE IF NOT EXISTS data_work_workflow_edge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工作流依赖边ID',
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    upstream_node_id BIGINT NOT NULL COMMENT '上游节点ID',
    upstream_node_code VARCHAR(100) NOT NULL COMMENT '上游节点编码',
    downstream_node_id BIGINT NOT NULL COMMENT '下游节点ID',
    downstream_node_code VARCHAR(100) NOT NULL COMMENT '下游节点编码',
    dependency_type VARCHAR(30) NOT NULL DEFAULT 'SCHEDULE' COMMENT '依赖类型：SCHEDULE',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    UNIQUE KEY uk_workflow_edge (workflow_id, upstream_node_code, downstream_node_code, deleted_at) COMMENT '工作流依赖边唯一',
    INDEX idx_workflow_id (workflow_id) COMMENT '工作流ID索引'
) COMMENT = '工作流依赖边表';

CREATE TABLE IF NOT EXISTS data_work_workflow_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工作流调度配置ID',
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
    enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用',
    scheduler_type VARCHAR(20) DEFAULT 'AIRFLOW' COMMENT '调度器类型：INTERNAL / AIRFLOW',
    next_execute_time DATETIME COMMENT '下次执行时间',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    UNIQUE KEY uk_workflow_schedule_workflow (workflow_id, deleted_at) COMMENT '工作流调度配置唯一',
    INDEX idx_workflow_id (workflow_id) COMMENT '工作流ID索引'
) COMMENT = '工作流调度配置表';

CREATE TABLE IF NOT EXISTS data_work_workflow_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工作流实例ID',
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    workflow_name VARCHAR(200) COMMENT '工作流名称快照',
    dag_id VARCHAR(200) NOT NULL COMMENT 'Airflow DAG ID',
    dag_run_id VARCHAR(250) NOT NULL COMMENT 'Airflow DAG Run ID',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：RUNNING / SUCCESS / FAILED',
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发类型：MANUAL / SCHEDULED',
    cost_time_ms BIGINT COMMENT '耗时，单位毫秒',
    error_message TEXT COMMENT '错误信息',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    UNIQUE KEY uk_workflow_dag_run (dag_id, dag_run_id, deleted_at) COMMENT 'DAG Run唯一索引',
    INDEX idx_workflow_id (workflow_id) COMMENT '工作流ID索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) COMMENT = '工作流实例表';

CREATE TABLE IF NOT EXISTS data_work_job_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '调度配置ID',
    job_id BIGINT NOT NULL COMMENT '关联的作业ID',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
    enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用',
    scheduler_type VARCHAR(20) DEFAULT 'AIRFLOW' COMMENT '调度器类型：INTERNAL / AIRFLOW',
    next_execute_time DATETIME COMMENT '下次执行时间',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    UNIQUE KEY uk_job_schedule_job (job_id, deleted_at) COMMENT '作业调度配置唯一',
    INDEX idx_job_id (job_id) COMMENT '作业ID索引'
) COMMENT = '作业调度配置表（新版），对应data_work_job';

CREATE TABLE IF NOT EXISTS data_work_job_dependency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业依赖ID',
    upstream_job_id BIGINT NOT NULL COMMENT '上游作业ID',
    downstream_job_id BIGINT NOT NULL COMMENT '下游作业ID',
    dependency_type VARCHAR(30) NOT NULL DEFAULT 'SCHEDULE' COMMENT '依赖类型：SCHEDULE',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME(6) DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    UNIQUE KEY uk_job_dependency (upstream_job_id, downstream_job_id, deleted_at) COMMENT '上下游依赖唯一索引',
    INDEX idx_upstream_job_id (upstream_job_id) COMMENT '上游作业ID索引',
    INDEX idx_downstream_job_id (downstream_job_id) COMMENT '下游作业ID索引'
) COMMENT = '作业依赖关系表，用于Airflow DAG编排和作业血缘';
