-- ============================================================
-- FinServe — Loan Origination System
-- Database Schema (MySQL 8.x)
-- ============================================================
-- Run this script to create the database and tables manually.
-- Alternatively, set spring.jpa.hibernate.ddl-auto=update
-- in application.properties and let Hibernate generate them.
-- ============================================================

CREATE DATABASE IF NOT EXISTS finserve_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE finserve_db;

-- -----------------------------------------------------------
-- Users table
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,          -- BCrypt hash
    phone       VARCHAR(20),
    role        ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',

    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- Loan Applications table
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS loan_applications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    tenure          INT             NOT NULL,           -- in months
    monthly_income  DECIMAL(15,2)   NOT NULL,
    employment_type VARCHAR(50)     NOT NULL,
    purpose         VARCHAR(255),
    status          ENUM('PENDING','APPROVED','REJECTED','UNDER_REVIEW')
                        NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_loan_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_loan_user   (user_id),
    INDEX idx_loan_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- Seed admin user (password: admin123, BCrypt-hashed)
-- -----------------------------------------------------------
INSERT IGNORE INTO users (name, email, password, phone, role)
VALUES (
    'Admin',
    'admin@finserve.com',
    '$2a$10$EqKcp1WFKbKOauLPouGYKe6PBMOmNHhkdSGo1DxhzlERjXmwZVqVe',
    '9999999999',
    'ADMIN'
);
