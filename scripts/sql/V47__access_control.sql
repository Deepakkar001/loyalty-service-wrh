-- Dynamic access control catalog + tenant RBAC (manual apply for prod; Hibernate ddl-auto syncs in dev)

CREATE TABLE IF NOT EXISTS access_module (
  module_key              VARCHAR(64)  NOT NULL PRIMARY KEY,
  display_name            VARCHAR(128) NOT NULL,
  nav_section             VARCHAR(64),
  sort_order              INT          NOT NULL DEFAULT 0,
  icon_key                VARCHAR(64),
  is_required             TINYINT(1)   NOT NULL DEFAULT 0,
  min_subscription_tier   VARCHAR(32),
  is_active               TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS access_action (
  action_key    VARCHAR(32)  NOT NULL PRIMARY KEY,
  display_name  VARCHAR(64)  NOT NULL,
  sort_order    INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS access_module_action (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  module_key      VARCHAR(64)  NOT NULL,
  action_key      VARCHAR(32)  NOT NULL,
  permission_key  VARCHAR(128) NOT NULL,
  is_assignable   TINYINT(1)   NOT NULL DEFAULT 1,
  UNIQUE KEY uk_module_action (module_key, action_key),
  UNIQUE KEY uk_permission_key (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS access_nav_item (
  id                           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  module_key                   VARCHAR(64)  NOT NULL,
  route_path                   VARCHAR(256) NOT NULL,
  label_key                    VARCHAR(128) NOT NULL,
  sort_order                   INT          NOT NULL DEFAULT 0,
  required_permission_key      VARCHAR(128) NOT NULL,
  requires_onboarding_complete TINYINT(1)   NOT NULL DEFAULT 1,
  icon_key                     VARCHAR(64),
  KEY idx_nav_module (module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tier_module_baseline (
  subscription_tier VARCHAR(32) NOT NULL,
  module_key        VARCHAR(64) NOT NULL,
  is_locked         TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (subscription_tier, module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_role_template (
  template_key     VARCHAR(64)  NOT NULL PRIMARY KEY,
  role_name        VARCHAR(128) NOT NULL,
  description      VARCHAR(512),
  permission_keys  JSON
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_module_entitlement (
  tenant_id   VARCHAR(64) NOT NULL,
  module_key  VARCHAR(64) NOT NULL,
  enabled     TINYINT(1)  NOT NULL DEFAULT 1,
  source      VARCHAR(32) NOT NULL,
  enabled_by  VARCHAR(128),
  enabled_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  notes       TEXT,
  PRIMARY KEY (tenant_id, module_key),
  KEY idx_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_role (
  role_id       VARCHAR(36)  NOT NULL PRIMARY KEY,
  tenant_id     VARCHAR(64)  NOT NULL,
  role_name     VARCHAR(128) NOT NULL,
  description   VARCHAR(512),
  is_system     TINYINT(1)   NOT NULL DEFAULT 0,
  template_key  VARCHAR(64),
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_role_name (tenant_id, role_name),
  KEY idx_tenant_role (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_user (
  user_id          VARCHAR(36)  NOT NULL PRIMARY KEY,
  tenant_id        VARCHAR(64)  NOT NULL,
  email            VARCHAR(255) NOT NULL,
  password_hash    VARCHAR(255),
  full_name        VARCHAR(255),
  status           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
  session_version  INT          NOT NULL DEFAULT 1,
  last_login_at    DATETIME(6),
  created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_email (tenant_id, email),
  KEY idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_user_role (
  user_id   VARCHAR(36) NOT NULL,
  role_id   VARCHAR(36) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_role_users (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS privilege_grant (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64)  NOT NULL,
  subject_type    VARCHAR(16)  NOT NULL,
  subject_id      VARCHAR(36)  NOT NULL,
  permission_key  VARCHAR(128) NOT NULL,
  effect          VARCHAR(16)  NOT NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_grant (tenant_id, subject_type, subject_id, permission_key),
  KEY idx_grant_subject (tenant_id, subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS access_control_audit_log (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id    VARCHAR(64),
  actor_type   VARCHAR(32),
  actor_id     VARCHAR(128),
  action       VARCHAR(64)  NOT NULL,
  payload_json JSON,
  created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_audit_tenant (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
