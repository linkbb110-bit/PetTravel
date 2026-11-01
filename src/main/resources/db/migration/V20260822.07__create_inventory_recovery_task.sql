CREATE TABLE inventory_compensation_task (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    task_status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL,
    last_error VARCHAR(500) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_task_order_type (order_id, task_type),
    KEY idx_inventory_task_retry (task_status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Redis/MySQL inventory recovery tasks';
