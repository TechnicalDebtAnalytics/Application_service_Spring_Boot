CREATE TABLE companies (
    company_id BIGSERIAL PRIMARY KEY,

    company_name VARCHAR(255) NOT NULL,

    github_organization_url VARCHAR(500),

    auth0_organization_id VARCHAR(255) UNIQUE,

    created_by BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_companies_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
        ON DELETE RESTRICT
);