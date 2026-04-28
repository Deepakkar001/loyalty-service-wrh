-- ─── TENANT AGREEMENTS ─────────────────────────────────────
-- Stores commercial terms signed by the tenant at Stage 2.
-- One tenant can have multiple agreement rows over time (superseded).
-- The APPROVED row is the active agreement.

CREATE TABLE tenant_agreements (
    id                      BIGINT UNSIGNED     AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)         NOT NULL,
    agreement_uid           VARCHAR(128)        NOT NULL,
    terms_version           VARCHAR(20)         NOT NULL,
    effective_date          DATE                NOT NULL,
    revenue_share_pct       DECIMAL(5,2)        NOT NULL,
    settlement_frequency    ENUM('DAILY','WEEKLY','MONTHLY','T_PLUS_1') NOT NULL,
    document_s3_key         VARCHAR(512),
    -- Signatory details (legal representative who signed)
    signed_by_name          VARCHAR(255)        NOT NULL,
    signed_by_email         VARCHAR(255)        NOT NULL,
    signed_by_designation   VARCHAR(255),
    signed_at               DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- Approval workflow
    status                  ENUM('PENDING_APPROVAL','APPROVED','REJECTED','SUPERSEDED')
                                NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_by_admin_id    VARCHAR(128),
    approved_at             DATETIME(6),
    rejection_reason        TEXT,
    created_at              DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_agreement_uid (agreement_uid),
    INDEX idx_tenant_id     (tenant_id),
    INDEX idx_status        (status),
    CONSTRAINT chk_revenue_share CHECK (revenue_share_pct >= 0 AND revenue_share_pct <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

