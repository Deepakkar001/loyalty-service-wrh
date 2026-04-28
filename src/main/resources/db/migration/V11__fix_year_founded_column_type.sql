-- Fix year_founded column type: SMALLINT UNSIGNED -> INT to match JPA Integer mapping
ALTER TABLE tenant_onboarding MODIFY COLUMN year_founded INT NULL;
