ALTER TABLE merchants
    ADD COLUMN capabilities_json JSON NULL DEFAULT '["campaigns","settlement","integration"]' AFTER eligible_categories_json;
