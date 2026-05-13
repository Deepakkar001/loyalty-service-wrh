-- ─── WEBHOOK SUBSCRIPTIONS + DELIVERY LOG ──────────────────
-- Canonical reference: LoyaltyOS_Master_Architecture_v3.docx.txt
-- This normalizes webhook configuration and enables delivery audit + retries.

CREATE TABLE webhook_subscriptions (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)      NOT NULL,
    endpoint_url            VARCHAR(2048)    NOT NULL,
    secret_vault_ref        VARCHAR(255)     NOT NULL,
    events_subscribed       JSON             NOT NULL,
    is_active               TINYINT(1)       NOT NULL DEFAULT 1,
    verification_status     ENUM('PENDING','VERIFIED','FAILED')
                                 NOT NULL DEFAULT 'PENDING',
    last_verified_at        DATETIME(6),
    created_at              DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)      ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_tenant_active (tenant_id, is_active),
    INDEX idx_tenant_status (tenant_id, verification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE webhook_delivery_log (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)      NOT NULL,
    subscription_id         BIGINT UNSIGNED  NOT NULL,
    event_type              VARCHAR(128)     NOT NULL,
    payload                 JSON             NOT NULL,
    status                  ENUM('PENDING','DELIVERED','FAILED','DLQ')
                                 NOT NULL DEFAULT 'PENDING',
    attempt_count           INT              NOT NULL DEFAULT 0,
    next_retry_at           DATETIME(6),
    last_error              TEXT,
    created_at              DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_sub_status (subscription_id, status, next_retry_at),
    INDEX idx_tenant_created (tenant_id, created_at),
    CONSTRAINT fk_webhook_delivery_subscription
        FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Undo (manual):
-- DROP TABLE webhook_delivery_log;
-- DROP TABLE webhook_subscriptions;

