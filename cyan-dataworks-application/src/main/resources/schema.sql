CREATE TABLE IF NOT EXISTS task_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件夹ID，主键，自增',
    name VARCHAR(200) NOT NULL COMMENT '文件夹名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_parent_id (parent_id) COMMENT '父文件夹索引，用于树形查询'
) COMMENT = '任务文件夹表，用于对数据加工任务进行分组组织';

CREATE TABLE IF NOT EXISTS data_work_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业ID，主键，自增',
    folder_id BIGINT DEFAULT 0 COMMENT '所属文件夹ID',
    name VARCHAR(200) NOT NULL COMMENT '作业名称',
    description VARCHAR(500) COMMENT '作业描述',
    engine_type VARCHAR(20) NOT NULL COMMENT '引擎类型：SPARK（SparkSQL）/ FLINK（FlinkSQL）',
    node_type VARCHAR(30) DEFAULT 'SPARK_SQL' COMMENT '节点类型：ODS_TO_DWD / SPARK_SQL / FLINK_SQL / PYTHON / DATA_QUALITY / VIRTUAL',
    sql_content TEXT NOT NULL COMMENT '用户编写的SELECT查询内容',
    config_json TEXT COMMENT '节点配置JSON，ODS_TO_DWD节点保存输入输出表、主键、op字段等配置',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '作业状态：DRAFT（草稿）/ ONLINE（已上线）/ OFFLINE（已下线）',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_folder_id (folder_id) COMMENT '文件夹索引',
    INDEX idx_name (name) COMMENT '作业名称索引，用于模糊搜索',
    INDEX idx_engine_type (engine_type) COMMENT '引擎类型索引，用于筛选'
) COMMENT = '数据加工作业表（新版），对标Spark/Flink的Job概念，存储作业定义';

CREATE TABLE IF NOT EXISTS data_work_job_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实例ID，主键，自增',
    job_id BIGINT NOT NULL COMMENT '关联的作业ID',
    job_name VARCHAR(200) COMMENT '作业名称（快照，防止作业改名后丢失历史名称）',
    engine_type VARCHAR(20) NOT NULL COMMENT '引擎类型：SPARK / FLINK',
    sql_content TEXT NOT NULL COMMENT 'SQL内容（快照，记录执行时的SQL）',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：RUNNING（运行中）/ SUCCESS（成功）/ FAILED（失败）',
    cost_time_ms BIGINT COMMENT '执行耗时，单位毫秒',
    result_data TEXT COMMENT '结果数据，JSON格式存储',
    error_message TEXT COMMENT '错误信息，执行失败时记录',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，即执行开始时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_job_id (job_id) COMMENT '作业ID索引，用于按作业查询实例',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引，用于时间范围查询'
) COMMENT = '作业实例表（新版），对标Spark的Task实例概念，存储每次执行的运行快照';

CREATE TABLE IF NOT EXISTS data_work_job_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '调度配置ID',
    job_id BIGINT NOT NULL UNIQUE COMMENT '关联的作业ID',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
    enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用',
    next_execute_time DATETIME COMMENT '下次执行时间',
    created_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system' COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间，逻辑删除标记',
    INDEX idx_job_id (job_id) COMMENT '作业ID索引'
) COMMENT = '作业调度配置表（新版），对应data_work_job';
