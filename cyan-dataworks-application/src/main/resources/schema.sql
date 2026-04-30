CREATE TABLE IF NOT EXISTS data_work_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    engine_type VARCHAR(20) NOT NULL,
    sql_content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    INDEX idx_name (name),
    INDEX idx_engine_type (engine_type)
);

CREATE TABLE IF NOT EXISTS schedule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE,
    cron_expression VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT FALSE,
    next_execute_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id)
);

CREATE TABLE IF NOT EXISTS execution_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_name VARCHAR(200),
    engine_type VARCHAR(20) NOT NULL,
    sql_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    cost_time_ms BIGINT,
    result_data TEXT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_created_at (created_at)
);
