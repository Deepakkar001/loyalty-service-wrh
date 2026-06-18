ALTER TABLE merchants
    ADD COLUMN eligible_categories_json JSON NULL AFTER commission_config;
