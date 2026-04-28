-- Extended company profile fields on tenant_onboarding
ALTER TABLE tenant_onboarding
    ADD COLUMN legal_business_name       VARCHAR(255)  NULL AFTER company_name,
    ADD COLUMN business_registration_no  VARCHAR(100)  NULL AFTER legal_business_name,
    ADD COLUMN sub_category              VARCHAR(100)  NULL AFTER business_category,
    ADD COLUMN business_model            VARCHAR(30)   NULL AFTER sub_category,
    ADD COLUMN number_of_locations       INT UNSIGNED  NULL AFTER business_model,
    ADD COLUMN headquarters_address      TEXT          NULL AFTER country_code,
    ADD COLUMN founder_names             VARCHAR(500)  NULL AFTER headquarters_address,
    ADD COLUMN year_founded              SMALLINT UNSIGNED NULL AFTER founder_names,
    ADD COLUMN annual_revenue_range      VARCHAR(20)   NULL AFTER year_founded,
    ADD COLUMN customer_base_size        INT UNSIGNED  NULL AFTER annual_revenue_range,
    ADD COLUMN payment_methods_accepted  VARCHAR(500)  NULL AFTER customer_base_size;

-- Financial & billing fields on tenant_agreements
ALTER TABLE tenant_agreements
    ADD COLUMN points_currency           VARCHAR(10)   NOT NULL DEFAULT 'INR' AFTER settlement_frequency,
    ADD COLUMN expected_daily_txn_volume INT UNSIGNED  NULL AFTER points_currency,
    ADD COLUMN billing_contact_name      VARCHAR(255)  NULL AFTER expected_daily_txn_volume,
    ADD COLUMN billing_address           TEXT          NULL AFTER billing_contact_name,
    ADD COLUMN payment_method            VARCHAR(30)   NULL AFTER billing_address,
    ADD COLUMN contract_duration_months  INT UNSIGNED  NOT NULL DEFAULT 12 AFTER payment_method,
    ADD COLUMN auto_renewal              TINYINT(1)    NOT NULL DEFAULT 1 AFTER contract_duration_months;
