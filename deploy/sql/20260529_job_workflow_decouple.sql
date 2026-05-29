-- DataWorks Job / Workflow 解耦
-- 说明：测试环境不保留旧 SINGLE_NODE workflow 兼容数据。

ALTER TABLE data_work_job_instance
    MODIFY COLUMN job_id BIGINT NULL COMMENT '关联的单节点作业ID，工作流节点实例可为空';

INSERT INTO data_work_job_schedule (job_id, cron_expression, enabled, scheduler_type, next_execute_time, created_by, created_at, updated_by, updated_at, deleted_at)
SELECT n.job_id,
       s.cron_expression,
       s.enabled,
       s.scheduler_type,
       s.next_execute_time,
       s.created_by,
       s.created_at,
       s.updated_by,
       s.updated_at,
       NULL
FROM data_work_workflow_schedule s
JOIN data_work_workflow w ON w.id = s.workflow_id AND w.workflow_type = 'SINGLE_NODE'
JOIN data_work_workflow_node n ON n.workflow_id = w.id AND n.deleted_at IS NULL
WHERE s.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM data_work_job_schedule js
      WHERE js.job_id = n.job_id
        AND js.deleted_at IS NULL
  );

DELETE FROM data_work_workflow_dependency;
DELETE FROM data_work_workflow_schedule;
DELETE FROM data_work_workflow_edge;
DELETE FROM data_work_workflow_node;
DELETE FROM data_work_workflow;

ALTER TABLE data_work_workflow_node
    DROP INDEX idx_job_id,
    DROP COLUMN job_id,
    ADD COLUMN engine_type VARCHAR(20) NOT NULL COMMENT '引擎类型：SPARK / FLINK / SHELL / PYTHON' AFTER node_name,
    ADD COLUMN node_type VARCHAR(30) NOT NULL COMMENT '节点类型：SPARK_SQL / FLINK_SQL / SPARK_BATCH / FLINK_BATCH / SHELL / PYTHON / DATA_QUALITY / VIRTUAL' AFTER engine_type,
    ADD COLUMN content TEXT NOT NULL COMMENT '节点内容，支持SQL、Shell、Python等正文' AFTER node_type;

ALTER TABLE data_work_workflow
    DROP INDEX idx_workflow_type,
    DROP COLUMN workflow_type;
