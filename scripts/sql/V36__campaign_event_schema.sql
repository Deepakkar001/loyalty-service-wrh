-- Manual schema change (Flyway not used). Run in MySQL Workbench when needed.
-- Adds campaigns.event_schema for per-campaign event payload definitions.

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'campaigns'
      AND COLUMN_NAME = 'event_schema'
);

SET @ddl = IF(
    @col_exists = 0,
    'ALTER TABLE campaigns ADD COLUMN event_schema JSON NULL AFTER trigger_event_type',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
