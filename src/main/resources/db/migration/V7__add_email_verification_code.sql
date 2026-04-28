-- Add code-based email verification (OTP)

ALTER TABLE tenant_onboarding
    ADD COLUMN email_verification_code_hash VARCHAR(255) NULL,
    ADD COLUMN email_verification_code_expiry DATETIME(6) NULL,
    ADD COLUMN email_verification_code_attempts INT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN email_verification_code_last_sent_at DATETIME(6) NULL,
    ADD INDEX idx_email_verification_code_expiry (email_verification_code_expiry);

