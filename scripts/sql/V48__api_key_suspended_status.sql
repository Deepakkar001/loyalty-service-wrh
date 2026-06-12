-- Adds SUSPENDED to tenant API key status (module-driven suspension without revoking keys).
-- MySQL ENUM alteration; safe if value already exists (re-run manually if needed).

ALTER TABLE tenant_api_key
    MODIFY COLUMN status ENUM('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE';
