-- Canonical programme configuration blob (versioned) for legacy Step 4.
-- This preserves backward compatibility while enabling JSON-schema validation
-- and Kafka config-update events for future engines.
--
-- NOTE: Version chosen to avoid collisions with existing V15/V16 migrations.

ALTER TABLE tenant_config
    ADD COLUMN programme_config JSON DEFAULT NULL,
    ADD COLUMN programme_config_version INT NOT NULL DEFAULT 0;

