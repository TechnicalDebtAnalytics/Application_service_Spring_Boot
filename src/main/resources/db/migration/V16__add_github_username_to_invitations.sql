ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS github_username VARCHAR(255);
