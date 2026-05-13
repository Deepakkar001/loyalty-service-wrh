-- ─── TENANT CONFIG: OPTIMISTIC LOCK COLUMN ─────────────────
-- Fixes Hibernate schema validation: TenantConfig entity uses @Version.
-- Add the version column additively (do not modify V13).

ALTER TABLE tenant_config
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 AFTER updated_at;

-- Undo (manual):
-- ALTER TABLE tenant_config DROP COLUMN version;

