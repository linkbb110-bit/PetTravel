CREATE TABLE camp_route (
    id BIGINT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    status INT NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE pet_info (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    breed VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    avatar_url VARCHAR(500),
    vaccine_status INT NOT NULL,
    character_tags VARCHAR(1000),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE appointment_order (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    order_status INT NOT NULL,
    pay_time DATETIME,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role_type INT NOT NULL,
    status INT NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
