-- Event field definitions live only in programme_config.config_json (eventSchema).
-- tenant_config.event_schema was unused in writes and only confused schemaPresent; remove it.

ALTER TABLE tenant_config
    DROP COLUMN event_schema;
