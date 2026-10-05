CREATE TABLE users (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                 VARCHAR(50)  NOT NULL,
    password_hash            VARCHAR(100) NOT NULL,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts          INT          NOT NULL DEFAULT 0,
    locked_until             DATETIME(6)  NULL,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE audit_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NULL,
    event      VARCHAR(40)  NOT NULL,
    detail     VARCHAR(255) NULL,
    ip_address VARCHAR(45)  NULL,
    created_at DATETIME(6)  NOT NULL
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
