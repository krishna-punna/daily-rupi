-- Run once as the MySQL root user. Replace CHANGE_ME with a strong password
-- and use the same value for DB_PASSWORD when starting the backend.
CREATE DATABASE IF NOT EXISTS daily_rupi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- The backend connects over TCP to 127.0.0.1. MySQL only maps that address to
-- 'localhost' when it resolves host names; servers started with skip-name-resolve
-- (the official Docker image, for one) see it as '127.0.0.1'. Create both so the
-- login works either way. Use the same password for both.
CREATE USER IF NOT EXISTS 'dailyrupi_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';
CREATE USER IF NOT EXISTS 'dailyrupi_app'@'127.0.0.1' IDENTIFIED BY 'CHANGE_ME';

-- Only this schema, and only what the app and its migrations need.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON daily_rupi.* TO 'dailyrupi_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON daily_rupi.* TO 'dailyrupi_app'@'127.0.0.1';

FLUSH PRIVILEGES;
