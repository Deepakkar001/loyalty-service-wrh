-- ─── ADMIN USERS ────────────────────────────────────────────
-- Platform administrators who review and approve/reject tenant agreements.
-- Separate from tenant_onboarding to enforce maker-checker role separation.

CREATE TABLE admin_users (
    id              BIGINT UNSIGNED     AUTO_INCREMENT PRIMARY KEY,
    admin_uid       VARCHAR(64)         NOT NULL,
    email           VARCHAR(255)        NOT NULL,
    password_hash   VARCHAR(255)        NOT NULL,
    full_name       VARCHAR(255)        NOT NULL,
    role            ENUM('PLATFORM_ADMIN','COMPLIANCE_OFFICER') NOT NULL,
    is_active       BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_admin_uid   (admin_uid),
    UNIQUE KEY uq_admin_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dev seed is handled by AdminDataInitializer.java at startup
-- (uses Spring's PasswordEncoder so the hash is always valid).
