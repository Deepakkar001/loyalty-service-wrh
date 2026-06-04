-- Campaign targeted audience (whitelist + upload batches). Run in MySQL when needed.

SET @col_scope = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'campaigns' AND COLUMN_NAME = 'customer_scope'
);
SET @ddl_scope = IF(
    @col_scope = 0,
    'ALTER TABLE campaigns ADD COLUMN customer_scope ENUM(''ALL'',''TARGETED'') NOT NULL DEFAULT ''ALL''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl_scope; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_count = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'campaigns' AND COLUMN_NAME = 'customer_count'
);
SET @ddl_count = IF(
    @col_count = 0,
    'ALTER TABLE campaigns ADD COLUMN customer_count INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @ddl_count; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS campaign_target_customers (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    campaign_uid        VARCHAR(128) NOT NULL,
    customer_id         VARCHAR(128) NOT NULL,
    added_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    added_by            VARCHAR(255) NULL,
    source_upload_uid   VARCHAR(128) NULL,
    UNIQUE KEY uk_campaign_customer (tenant_id, campaign_uid, customer_id),
    INDEX idx_tenant_campaign (tenant_id, campaign_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campaign_target_uploads (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    campaign_uid        VARCHAR(128) NOT NULL,
    upload_uid          VARCHAR(128) NOT NULL,
    original_filename   VARCHAR(255) NULL,
    file_size_bytes     BIGINT NULL,
    file_sha256         VARCHAR(64) NULL,
    status              ENUM('PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PROCESSING',
    total_rows_uploaded INT NOT NULL DEFAULT 0,
    imported_count      INT NOT NULL DEFAULT 0,
    duplicate_count     INT NOT NULL DEFAULT 0,
    error_count         INT NOT NULL DEFAULT 0,
    error_report_json   JSON NULL,
    uploaded_by         VARCHAR(255) NULL,
    uploaded_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at        DATETIME(6) NULL,
    UNIQUE KEY uk_upload_uid (tenant_id, upload_uid),
    INDEX idx_tenant_campaign (tenant_id, campaign_uid),
    INDEX idx_tenant_file_sha (tenant_id, file_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
