-- Scope tiers to a programme.
-- Backward compatible: existing code can still query by tenant_id only.
--
-- NOTE: Version chosen to avoid collisions with existing V15/V16 migrations.

ALTER TABLE tier_definitions
    ADD COLUMN programme_uid VARCHAR(64) DEFAULT NULL,
    ADD INDEX idx_tenant_programme_rank (tenant_id, programme_uid, rank_order);

-- Backfill: create a default programme per tenant that already has tier definitions,
-- and associate all existing tiers to that default programme.
INSERT INTO programmes (tenant_id, programme_uid, name, status, active_config_version)
SELECT DISTINCT td.tenant_id, 'default', tc.display_name, 'DRAFT', 0
FROM tier_definitions td
LEFT JOIN tenant_config tc ON tc.tenant_id = td.tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM programmes p
    WHERE p.tenant_id = td.tenant_id AND p.programme_uid = 'default'
);

UPDATE tier_definitions
SET programme_uid = 'default'
WHERE programme_uid IS NULL;

