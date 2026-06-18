-- Structured merchant partner agreements (mirrors tenant_agreements pattern).
-- merchants.agreement_document_url / agreement_accepted_at remain for backward compatibility.

CREATE TABLE merchant_agreements (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   VARCHAR(64) NOT NULL,
    merchant_uid                VARCHAR(128) NOT NULL,
    agreement_uid               VARCHAR(128) NOT NULL,
    terms_version               VARCHAR(20) NOT NULL,
    effective_date              DATE NOT NULL,
    revenue_share_pct           DECIMAL(5, 2) NOT NULL,
    settlement_cycle            VARCHAR(32) NOT NULL,
    points_currency             VARCHAR(10) NOT NULL DEFAULT 'INR',
    expected_daily_txn_volume   INT NULL,
    billing_contact_name        VARCHAR(255) NULL,
    billing_address             TEXT NULL,
    payment_method              VARCHAR(30) NULL,
    contract_duration_months    INT NOT NULL DEFAULT 12,
    auto_renewal                TINYINT(1) NOT NULL DEFAULT 1,
    proposed_earn_rate_multiplier DECIMAL(6, 3) NULL,
    merchant_funded_campaigns_allowed TINYINT(1) NOT NULL DEFAULT 1,
    signed_by_name              VARCHAR(255) NOT NULL,
    signed_by_email             VARCHAR(255) NOT NULL,
    signed_by_designation       VARCHAR(255) NULL,
    signed_at                   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_by_email          VARCHAR(255) NOT NULL,
    status                      VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    created_at                  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_merchant_agreement_uid (agreement_uid),
    INDEX idx_merchant_agreements_merchant (tenant_id, merchant_uid)
);
