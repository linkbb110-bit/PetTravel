DROP TABLE IF EXISTS inventory_compensation_task;
DROP TABLE IF EXISTS appointment_order;
DROP TABLE IF EXISTS pet_info;
DROP TABLE IF EXISTS camp_route;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE camp_route (
    id BIGINT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    status INT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0,
    CHECK (available_stock >= 0),
    CHECK (available_stock <= total_stock)
);

CREATE TABLE pet_info (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    breed VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    avatar_url VARCHAR(500),
    vaccine_status INT NOT NULL,
    character_tags VARCHAR(1000),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE appointment_order (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    order_status INT NOT NULL,
    pay_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0,
    active_booking_key INT GENERATED ALWAYS AS (
        CASE WHEN order_status IN (0, 1) AND is_deleted = 0 THEN 1 ELSE NULL END
    ),
    CONSTRAINT uk_order_user_route_active UNIQUE(user_id, route_id, active_booking_key)
);
CREATE INDEX idx_order_active ON appointment_order(user_id, route_id, order_status, is_deleted);
CREATE INDEX idx_order_timeout ON appointment_order(order_status, is_deleted, create_time);

CREATE TABLE inventory_compensation_task (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    task_status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP NOT NULL,
    last_error VARCHAR(500),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    CONSTRAINT uk_inventory_task_order_type UNIQUE(order_id, task_type)
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role_type INT NOT NULL,
    status INT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0
);
