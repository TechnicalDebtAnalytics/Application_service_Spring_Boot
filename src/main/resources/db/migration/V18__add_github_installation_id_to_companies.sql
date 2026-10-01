-- Add github_installation_id column to companies table for GitHub App integration
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS github_installation_id BIGINT;
