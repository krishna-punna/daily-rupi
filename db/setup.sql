-- Run once as the MySQL root user. Replace CHANGE_ME with a strong password
-- and use the same value for DB_PASSWORD when starting the backend.
CREATE DATABASE IF NOT EXISTS daily_rupi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'dailyrupi_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';

-- Only this schema, and only what the app and its migrations need.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON daily_rupi.* TO 'dailyrupi_app'@'localhost';

FLUSH PRIVILEGES;
