-- ==========================================
-- Technical Debt Analytics Database Schema
-- ==========================================

-- 1. users table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    auth0_user_id VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    github_username VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. company table
CREATE TABLE IF NOT EXISTS company (
    company_id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    github_organization_url VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_created_by FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. super_admin table
CREATE TABLE IF NOT EXISTS super_admin (
    super_admin_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_super_admin_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_super_admin_company FOREIGN KEY (company_id) REFERENCES company(company_id) ON DELETE CASCADE
);

-- 4. repository table
CREATE TABLE IF NOT EXISTS repository (
    repository_id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    github_repository_id VARCHAR(255) NOT NULL UNIQUE,
    repository_name VARCHAR(255) NOT NULL,
    repository_url VARCHAR(255) NOT NULL UNIQUE,
    default_branch VARCHAR(255) DEFAULT 'main',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repository_company FOREIGN KEY (company_id) REFERENCES company(company_id) ON DELETE CASCADE
);
