ALTER TABLE tenant_user
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 AFTER session_version;
