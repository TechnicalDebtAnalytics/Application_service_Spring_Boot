CREATE TABLE repo_assignments (
    member_id BIGINT NOT NULL,
    repository_id BIGINT NOT NULL,

    created_at TIMESTAMP(6) WITHOUT TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- The relationship itself is uniquely identified by its Member/Repository
    -- pair, preventing duplicate access assignments without a surrogate ID.
    CONSTRAINT pk_repo_assignments
        PRIMARY KEY (member_id, repository_id),

    -- Removing a Member also removes repository access owned by that membership.
    CONSTRAINT fk_repo_assignments_member
        FOREIGN KEY (member_id)
        REFERENCES members(member_id)
        ON DELETE CASCADE,

    -- Removing a Repository also removes every assignment targeting it.
    CONSTRAINT fk_repo_assignments_repository
        FOREIGN KEY (repository_id)
        REFERENCES repositories(repository_id)
        ON DELETE CASCADE
);

-- The composite primary key begins with member_id. This additional index
-- supports reverse lookups and foreign-key operations by repository_id.
CREATE INDEX idx_repo_assignments_repository_id
    ON repo_assignments (repository_id);
