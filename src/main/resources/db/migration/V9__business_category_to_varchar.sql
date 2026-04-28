-- Change business_category from ENUM to VARCHAR(64) to support dynamic categories.
ALTER TABLE tenant_onboarding
    MODIFY COLUMN business_category VARCHAR(64) NOT NULL;
