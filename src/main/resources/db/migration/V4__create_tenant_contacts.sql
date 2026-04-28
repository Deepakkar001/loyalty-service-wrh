-- ─── TENANT CONTACTS ───────────────────────────────────────
-- Every tenant must have at minimum one PRIMARY_ADMIN and one TECHNICAL_POC.
-- These contacts receive platform notifications, billing alerts, and API keys.

CREATE TABLE tenant_contacts (
    id              BIGINT UNSIGNED     AUTO_INCREMENT PRIMARY KEY,
    tenant_id       VARCHAR(64)         NOT NULL,
    contact_uid     VARCHAR(128)        NOT NULL,
    name            VARCHAR(255)        NOT NULL,
    email           VARCHAR(255)        NOT NULL,
    phone           VARCHAR(30),
    designation     VARCHAR(255),
    role            ENUM('PRIMARY_ADMIN','TECHNICAL_POC','BUSINESS_POC',
                         'BILLING_POC','SUPPORT_POC') NOT NULL,
    is_primary      TINYINT(1)          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_contact_uid   (contact_uid),
    INDEX idx_tenant_id         (tenant_id),
    INDEX idx_role              (tenant_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

