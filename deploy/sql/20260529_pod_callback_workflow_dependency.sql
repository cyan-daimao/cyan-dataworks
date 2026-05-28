ALTER TABLE data_work_job_instance
    ADD COLUMN runtime_job_name varchar(128) NULL COMMENT '运行时Kubernetes Job名称' AFTER scheduler_try_number,
    ADD COLUMN log_object_key varchar(512) NULL COMMENT 'RustFS日志对象Key' AFTER runtime_job_name,
    ADD COLUMN started_at datetime(6) NULL COMMENT '运行开始时间' AFTER log_object_key,
    ADD COLUMN finished_at datetime(6) NULL COMMENT '运行结束时间' AFTER started_at,
    ADD COLUMN callback_at datetime(6) NULL COMMENT '回调时间' AFTER finished_at;

CREATE TABLE IF NOT EXISTS data_work_workflow_dependency (
    id bigint NOT NULL COMMENT '主键',
    upstream_workflow_id bigint NOT NULL COMMENT '上游工作流ID',
    downstream_workflow_id bigint NOT NULL COMMENT '下游工作流ID',
    dependency_type varchar(64) NOT NULL DEFAULT 'SCHEDULE_SAME_CYCLE' COMMENT '依赖类型',
    created_by varchar(64) NULL COMMENT '创建人',
    created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_by varchar(64) NULL COMMENT '更新人',
    updated_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    deleted_at datetime(6) NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_workflow_dependency (upstream_workflow_id, downstream_workflow_id, deleted_at),
    KEY idx_workflow_dependency_upstream (upstream_workflow_id),
    KEY idx_workflow_dependency_downstream (downstream_workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DataWorks工作流级依赖';
