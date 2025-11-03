-- Database schema for JDBC tests
-- This creates the tasks table for testing

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL
);
