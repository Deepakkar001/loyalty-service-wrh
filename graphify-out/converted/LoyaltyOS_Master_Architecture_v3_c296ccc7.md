<!-- converted from LoyaltyOS_Master_Architecture_v3.docx -->


LOYALTYOS PLATFORM
Master Architecture & Developer Build Specification
Version 3.0  |  MySQL-First  |  Gap-Resolved  |  Cursor-Ready


This document is the single source of truth for building LoyaltyOS. Every BRD requirement is mapped to a specific MySQL table, Spring Boot service, Kafka topic, Redis key, and API endpoint. All 5 architecture gaps identified in the review have been resolved with production-grade designs.

# 1. Executive Summary & Document Purpose

This document supersedes all previous architecture versions. It is written specifically for the development team (and Cursor AI) to have unambiguous, implementation-ready answers for every BRD requirement. For every module it answers: what to build, which MySQL tables and columns to create, which Spring Boot service owns it, which Kafka topic carries its events, which Redis keys support it, which API endpoints expose it, and who (which role) can call each endpoint.


## 1.1 Technology Stack (Confirmed — No Deviations)

# 2. MySQL 8.0 — Complete Strategy & Tenant Isolation

## 2.1 Why MySQL Replaces Both PostgreSQL and MongoDB

## 2.2 Tenant Isolation — 5-Layer RLS Equivalent
MySQL has no native Row-Level Security. The following five independently enforced layers together provide stronger guarantees than a single PostgreSQL RLS policy because each layer is tested in CI independently.


## 2.3 MySQL Connection Pool — HikariCP Configuration

# 3. Complete MySQL Schema — All Tables


## 3.1 Core Identity & Configuration Tables
### tenant_config — Master tenant registry

### customer_profiles — Unified customer identity (replaces MongoDB)

### customer_identity_links — CDP multi-source identity resolution

## 3.2 Points Ledger — Immutable Append-Only

### customer_balance_snapshot — L2 materialized balance cache

## 3.3 Rules & Rule Versioning Tables

## 3.4 Tier Management Tables

## 3.5 Campaign, Promotion & Coupon Tables

## 3.6 Referral Programme Tables

## 3.7 Merchant & Partner Tables

## 3.8 Gamification Tables

## 3.9 Notifications, Webhooks & Audit Tables

## 3.10 Approval Workflow Table (Maker-Checker)

## 3.11 Fraud Detection Tables

## 3.12 Coalition Tables

# 4. Architecture Gap Fixes — All 5 Resolved


## 4.1 GAP FIX #1 — Rule Simulation Tool
BRD Requirement (4.8.3): 'A rule simulation tool allows users to test a new rule against historical data before go-live — estimating liability and customer impact.'

### What to Build
- RuleSimulationService — new Spring Boot service method (within Rule Engine Service)
- Reads historical event data from ClickHouse (read-only, never from MySQL transactional DB)
- Runs SpEL evaluation in DRY_RUN mode — no ledger writes, no Kafka publish, no Redis mutation
- Stores results in rule_simulation_results MySQL table (Section 3.3)
- Exposed via Business Portal UI as a 'Simulate' button on any DRAFT rule

### API Endpoint

### Processing Flow

## 4.2 GAP FIX #2 — External Partner Redemption Pending State
BRD Requirement (4.12.3): 'If the partner API is unavailable, points are placed in a pending state and the system retries automatically at defined intervals.'

### What to Build
- PartnerRedemptionService — owns the entire lifecycle of cross-partner redemptions (airline miles, hotel points, gift cards)
- partner_redemption_requests table tracks state machine: RESERVED → PENDING_PARTNER → COMPLETED or FAILED → REVERSED
- Spring Batch RetryPartnerRedemptionJob runs every 5 minutes — picks up PENDING_PARTNER rows and retries
- Exponential backoff: 1min → 5min → 15min → 1hr → 4hr → 24hr. After 24h: DLQ + ops alert + notify customer

### State Machine & Flow

### Partner Configuration in MySQL

## 4.3 GAP FIX #3 — Rule Versioning & Rollback
BRD Requirement (4.8.3): 'All rule changes are versioned — the business can roll back to a previous version within 24 hours.'

### What to Build
- loyalty_rules_history table (Section 3.3) — stores full JSON snapshot before every rule UPDATE
- RuleVersioningInterceptor — Spring AOP @Before on RuleService.update() and RuleService.updateStatus()
- RuleRollbackService — accepts (tenantId, ruleUid, targetVersion), restores snapshot, increments version, fires Kafka config-update


## 4.4 GAP FIX #4 — 24-Month Inactivity Flag
BRD Requirement (4.1.4): 'A profile inactive for more than 24 months must be flagged before any new points are awarded.'

### What to Build
- last_activity_at DATETIME(6) column on customer_profiles — updated on EVERY qualifying event receipt
- inactivity_flag TINYINT(1) column on customer_profiles — set by nightly batch job
- InactivityFlagBatchJob — Spring Batch, runs nightly at 02:00 UTC: UPDATE customer_profiles SET inactivity_flag=1 WHERE last_activity_at < DATE_SUB(NOW(), INTERVAL 24 MONTH) AND inactivity_flag=0
- Reward Engine pre-check: before awarding points, read inactivity_flag from MySQL (NOT from Redis — must be authoritative)
- If flagged=1: publish CustomerInactiveRewardAttempt Kafka event → Notification Service sends re-engagement push + email → Points are STILL awarded after notification (BRD says 'flagged before' not 'blocked')


## 4.5 GAP FIX #5 — Campaign Budget Atomic Decrement (Race Condition)
BRD Requirement (4.9.3): 'Each campaign has a defined budget — the platform stops the campaign automatically when the budget is exhausted.'
The Risk: SELECT then UPDATE pattern causes race condition under concurrent load — campaign can overspend.

### Production-Grade Fix: Atomic MySQL UPDATE

# 5. Dual Customer Ingestion — Full Implementation Spec


## 5.1 Ingestion API — Both Modes

## 5.2 Ingestion Processing Logic — CustomerProfileService

# 6. Coalition Module — Cross-Platform Loyalty


## 6.1 Coalition Lifecycle

## 6.2 ACID Transfer — Critical Code

# 7. BRD Functional Requirements — Complete Build Specification

Every module from BRD Section 4 is mapped below to: MySQL tables, Spring Boot service, Kafka topics, Redis keys, API endpoints, and access roles. This is the Cursor build specification.

## 7.1 Customer Profile Management (BRD 4.1)

## 7.2 Customer Enrolment & Membership (BRD 4.2)

## 7.3 Tier Management (BRD 4.3)

## 7.4 Loyalty Wallet (BRD 4.4)

## 7.5 Points Earning — Accrual (BRD 4.5)

## 7.6 Points Redemption (BRD 4.6)

## 7.7 Points Expiry Management (BRD 4.7)

## 7.8 Rules Engine (BRD 4.8)

## 7.9 Campaigns & Promotions (BRD 4.9)

## 7.10 Coupon Management (BRD 4.10)

## 7.11 Referral Programme (BRD 4.11)

## 7.12 Rewards Catalogue & Inventory (BRD 4.12)

## 7.13 Merchant Onboarding (BRD 4.13)

## 7.14 Merchant Self-Service Campaign Portal (BRD 4.14)
- Separate authentication: merchants log in via merchant_credentials table — NOT the Business Portal JWT
- Merchant can create campaigns scoped to their merchant_uid — same campaigns table, merchant_id FK populated
- All merchant-created campaigns go through approval_requests workflow before status=ACTIVE
- Merchant dashboard: GET /v1/{t}/merchants/{uid}/campaigns — filtered by their merchant_uid
- 80% budget alert sent to merchant email (notification_log + email gateway)
- Merchants CANNOT see other merchants' data — tenantId + merchant_uid double-scoping on all queries

## 7.15 Merchant Settlement (BRD 4.15)

## 7.16 Refunds & Reversals (BRD 4.16)
- Transaction reversed: INSERT points_ledger REVERSAL entry (negative of original CREDIT) — idempotency_key = 'rev_'+original_idem_key
- Redemption reversed: INSERT REVERSAL entry to return points — balance immediately restored
- Partial refund: INSERT REVERSAL for proportionate amount — computed as (refundAmount / originalAmount) × originalPoints
- Manual adjustment: INSERT MANUAL_CREDIT or MANUAL_DEBIT — requires reason_code + approved_by (maker-checker)
- Every reversal traceable to original transaction via source_event_id on ledger row


## 7.17 Loyalty Liability Management (BRD 4.17)

## 7.18 Fraud Detection (BRD 4.18)

## 7.19 Administration & RBAC (BRD 4.19)

## 7.20 Notification Engine (BRD 4.20)

- NotificationService consumes from platform.notifications Kafka topic
- Before sending: check communication_prefs JSON field on customer_profiles for non-transactional messages
- Delivery status logged in notification_log with provider_ref from SMS/email gateway
- Templates stored in notification_templates table — editable from Business Portal, no redeploy needed
- Personalisation: {customerName}, {points}, {expiryDate}, {tierName} replaced at send time

## 7.21 Event & Webhook Engine (BRD 4.21)
- WebhookDeliveryService subscribes to all outbound events, looks up tenant webhook_subscriptions, POSTs to endpoint
- Retry schedule (webhook_delivery_log): 1min → 5min → 7min → 2h → 12h. After 5 failures: status=DLQ, admin alerted
- Tenants can view delivery history in Business Portal: GET /v1/{t}/webhooks/deliveries
- Manual retry: POST /v1/{t}/webhooks/deliveries/{id}/retry (ADMIN only)

## 7.22 Reporting & Analytics (BRD 4.23)
- All reports exportable to CSV/XLSX via GET ?format=csv or ?format=xlsx query param
- Scheduled delivery: report_schedules table stores cron expression + recipient emails + S3 path
- ClickHouse used for all analytics queries — MySQL never queried for historical aggregations at scale

# 8. Redis Key Catalogue — Complete

Every Redis key used in the platform is documented here. Cursor must implement all key patterns exactly as specified. TTL values are mandatory — no Redis key should exist without an expiry.


## 8.1 Redis Cluster Configuration

# 9. Kafka Topic Catalogue — Complete


## 9.1 Kafka Consumer Group Strategy

# 10. Spring Boot Service Decomposition

The platform is decomposed into the following Spring Boot microservices. Each service owns a specific domain and its MySQL tables. No service reads another service's tables directly — communication is via Kafka or internal REST calls.


## 10.1 Service Communication Rules
- Async (preferred): Services communicate via Kafka topics for all non-latency-critical operations
- Sync (allowed): Service A may call Service B via REST only for real-time lookups (e.g. BalanceService lookup during redemption)
- Database isolation: Each service gets its own MySQL schema user with INSERT+SELECT grants only on its own tables
- No shared ORM entities: Each service has its own JPA entities for the tables it owns — no cross-service ORM imports
- Config: All service configs loaded from Spring Cloud Config Server (backed by Git) — no hardcoded values

# 11. Performance Targets & MySQL Optimization


## 11.1 MySQL Index Strategy — Critical Queries

# 12. Zero-Downtime Migration Plan



# 13. UAT Acceptance Criteria — All BRD Modules



# 14. Frontend — Next.js 14 Build Specification


## 14.1 Business Portal Tech Stack
- Framework: Next.js 14 App Router — all data-heavy pages use React Server Components (data fetched on server, PII never in browser bundle)
- UI Components: ShadCN UI (Radix primitives) + Tailwind CSS — accessible, white-label ready (CSS variables for tenant branding)
- State: TanStack Query v5 for server state (API data); Zustand for UI state (sidebar, modals)
- Charts: Recharts for real-time metrics; Nivo for cohort/funnel analytics
- Real-time: Server-Sent Events (SSE) via Next.js Route Handler for live dashboard metrics
- Auth: NextAuth.js 5 + Keycloak — supports SAML 2.0, OIDC, MFA enforcement per role
- Forms: React Hook Form + Zod validation — Rule Builder uses drag-and-drop (dnd-kit)
- Tables: TanStack Table v8 — virtual scrolling for large customer lists
- i18n: next-intl — English + Hindi at minimum; extensible per tenant language preference

# 15. Glossary


LoyaltyOS Platform  |  Master Architecture & Build Specification v3.0  |  CONFIDENTIAL
© 2025 LoyaltyOS Inc. All rights reserved. This document is the single source of truth for all development work.
| Version | 3.0.0 |
| --- | --- |
| Date | June 2025 |
| Status | FINAL — FOR DEVELOPMENT |
| Stack | Java 21 · Spring Boot 3.3 · Kafka · Redis · MySQL 8.0 · Next.js 14 |
| Audience | Engineering / Cursor AI / Architecture Review |
| Scope of This Document
1. Complete MySQL schema (all tables, columns, indexes, constraints, triggers)
2. All 5 architecture gaps from review — now fully resolved with production designs
3. Every BRD module (4.1–4.23) mapped to implementation components
4. API endpoint catalogue with method, path, auth, request/response shape
5. Kafka topic catalogue with producer, consumer, payload schema
6. Redis key catalogue with pattern, TTL, and owning service
7. Spring Boot service decomposition — which service owns which domain
8. Role-based access control matrix for every API
9. Coalition (cross-platform) module full design
10. Dual customer ingestion (ID-only + full-profile) full design |
| --- |
| Layer | Technology | Version | Notes |
| --- | --- | --- | --- |
| Backend Services | Java + Spring Boot | 21 / 3.3 | Virtual threads (Project Loom). All microservices. |
| Event Streaming | Apache Kafka | 3.x KRaft | No ZooKeeper. Exactly-once semantics for Reward Engine. |
| Primary Database | MySQL (Aurora MySQL) | 8.0+ | ALL data. Replaces PostgreSQL + MongoDB. |
| Cache / Counters | Redis Cluster | 7.2 | Rules cache, balance cache, idempotency, freq counters, streaks. |
| Analytics | ClickHouse | Cloud / self-hosted | Kafka-fed. Never queried by transactional services. |
| Frontend | Next.js + React + Tailwind | 14 / 18 / 3.x | App Router, Server Components, NextAuth.js 5. |
| API Gateway | Kong Gateway | 3.x | JWT auth, rate limiting, tenant routing. |
| Secrets | HashiCorp Vault | 1.15+ | All DB creds, API keys, webhook secrets, PII DEKs. |
| Container Orch. | Kubernetes + KEDA | 1.30 / 2.x | KEDA scales pods on Kafka consumer lag. |
| DB Migrations | Flyway | 10.x | MySQL dialect. Versioned SQL scripts. |
| Connection Pool | HikariCP | (Spring Boot default) | max-pool-size=50 per pod. MySQL tuned settings. |
| Observability | Grafana + Prometheus + Loki + Tempo | LGTM stack | OpenTelemetry instrumentation on all services. |
| Requirement | Original DB | MySQL 8.0 Solution | Key MySQL Feature |
| --- | --- | --- | --- |
| ACID ledger transactions | PostgreSQL | InnoDB ACID + INSERT-only trigger | InnoDB serializable isolation |
| Row-Level Security | PostgreSQL RLS | 5-layer app + DB enforcement (Section 2.2) | JPA interceptor + DB user privileges |
| Flexible schema / JSON docs | MongoDB | JSON columns + Generated Column indexes | JSON_EXTRACT() + Virtual Generated Columns |
| Full-text customer search | MongoDB Atlas Search | MySQL FULLTEXT index on InnoDB | MATCH() AGAINST() on FULLTEXT index |
| Rule condition trees (AST) | PostgreSQL JSONB | MySQL JSON column | JSON_EXTRACT for query, SpEL for eval |
| Tenant config as document | MongoDB | MySQL JSON column in tenant_config | Same row, relational joins available |
| Time-series analytics | ClickHouse (unchanged) | ClickHouse unchanged — Kafka-fed | No MySQL involvement in analytics reads |
| Graph: referral tree | Neo4j | MySQL Recursive CTE | WITH RECURSIVE ... for chain traversal |
| Layer | Mechanism | Enforced At | What Happens on Failure |
| --- | --- | --- | --- |
| 1 — JPA Interceptor | Hibernate EmptyInterceptor.onPrepareStatement() rewrites every SQL WHERE clause to inject AND tenant_id = :currentTenantId | JPA / Hibernate | Query throws TenantViolationException → 403 response → PagerDuty alert |
| 2 — AOP Service Assertion | @TenantScope annotation + Spring AOP aspect validates tenantId in method arguments matches TenantContext (populated from JWT) | Spring service layer | RuntimeException → 403 → audit log entry |
| 3 — MySQL DB User Privilege | Each enterprise tenant gets a dedicated MySQL user (tenant_{id}_user) with SELECT/INSERT on rows only via VIEW. Standard tenants use shared user but Views filter by tenant_id. | MySQL engine | Connection refused for wrong user |
| 4 — Integration Test Suite | CI pipeline: cross-tenant-isolation-test.java — inserts data for Tenant A, queries as Tenant B, asserts 0 rows returned. Runs on every PR. | GitHub Actions CI | Build fails → PR cannot merge |
| 5 — Kong API Gateway | JWT validation at gateway. tenantId extracted from JWT sub claim and injected as X-Tenant-ID header. Services trust only this header. | Network / API Gateway | Request rejected before reaching any service |
| # application.yml — MySQL + HikariCP production settings
spring.datasource.url: jdbc:mysql://aurora-cluster:3306/loyaltyos?useSSL=true&requireSSL=true&serverTimezone=UTC
spring.datasource.hikari.maximum-pool-size: 50
spring.datasource.hikari.minimum-idle: 10
spring.datasource.hikari.connection-timeout: 30000
spring.datasource.hikari.idle-timeout: 600000
spring.datasource.hikari.max-lifetime: 1800000
spring.datasource.hikari.leak-detection-threshold: 60000
spring.jpa.hibernate.ddl-auto: validate   # Flyway owns schema — never auto-create
spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.MySQL8Dialect |
| --- |
| Schema Conventions
• Every table has tenant_id VARCHAR(64) as the second column (after primary key) — all queries must include it
• All primary keys are BIGINT UNSIGNED AUTO_INCREMENT — never exposed in APIs (use *_uid VARCHAR fields instead)
• Immutable tables (ledger, audit_log) have BEFORE UPDATE and BEFORE DELETE triggers that SIGNAL SQLSTATE '45000'
• All DATETIME fields use DATETIME(6) for microsecond precision
• All monetary values use DECIMAL(18,2); all point values use DECIMAL(18,4)
• JSON columns store flexible/dynamic data; Generated Columns create indexes on frequently-queried JSON paths
• Engine=InnoDB on all tables — required for ACID, row-level locking, and foreign key support |
| --- |
| CREATE TABLE tenant_config (
  id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id             VARCHAR(64) NOT NULL UNIQUE,
  display_name          VARCHAR(255) NOT NULL,
  subscription_tier     ENUM('STANDARD','PROFESSIONAL','ENTERPRISE') NOT NULL DEFAULT 'STANDARD',
  status                ENUM('ACTIVE','SUSPENDED','OFFBOARDED') NOT NULL DEFAULT 'ACTIVE',
  ingestion_modes       ENUM('ID_ONLY','FULL_PROFILE','BOTH') NOT NULL DEFAULT 'BOTH',
  points_currency_rate  DECIMAL(10,6) NOT NULL DEFAULT 0.010000,  -- 1pt = INR 0.01
  daily_points_cap      DECIMAL(18,4),   -- safety cap per tenant per day
  feature_flags         JSON NOT NULL DEFAULT ('{}'),
  -- feature_flags keys: tiersEnabled, aiEnabled, coalitionEnabled, gamificationEnabled,
  --   referralEnabled, merchantPortalEnabled, nftEnabled, analyticsAssistantEnabled
  event_schema          JSON,            -- tenant-defined event field definitions
  webhook_config        JSON,            -- {endpointUrl, secretVaultRef, retryPolicy}
  branding              JSON,            -- {logoUrl, primaryColor, secondaryColor, emailTemplateId}
  data_residency_region ENUM('IN','US','EU','APAC') NOT NULL DEFAULT 'IN',
  max_active_rules      INT NOT NULL DEFAULT 50,  -- tier-based limit
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE customer_profiles (
  id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id             VARCHAR(64) NOT NULL,
  customer_uid          VARCHAR(128) NOT NULL,   -- internal platform UUID
  external_id           VARCHAR(256),            -- client's own customer ID
  ingestion_mode        ENUM('ID_ONLY','FULL_PROFILE') NOT NULL DEFAULT 'FULL_PROFILE',
  -- PII fields (AES-256 encrypted at application layer before INSERT)
  first_name_enc        VARBINARY(512),
  last_name_enc         VARBINARY(512),
  email_enc             VARBINARY(512),
  phone_enc             VARBINARY(256),
  date_of_birth_enc     VARBINARY(64),
  -- Searchable non-PII hashes for lookup
  email_hash            VARCHAR(64),   -- SHA-256 of lowercase email for dedup
  phone_hash            VARCHAR(64),   -- SHA-256 of normalized phone for dedup
  -- Profile metadata
  tier_id               VARCHAR(64),
  tier_assigned_at      DATETIME(6),
  tier_protected_until  DATETIME(6),  -- grace period end date
  status                ENUM('ACTIVE','SUSPENDED','ANONYMISED') NOT NULL DEFAULT 'ACTIVE',
  inactivity_flag       TINYINT(1) NOT NULL DEFAULT 0,  -- set by nightly batch if >24mo inactive
  last_activity_at      DATETIME(6),  -- updated on every qualifying event
  custom_attributes     JSON,         -- tenant-defined dynamic fields
  communication_prefs   JSON,         -- {email:true, sms:false, push:true}
  segmentation_tags     JSON,         -- ['High Value','At Risk of Churn']
  consent_flags         JSON,         -- {aiProcessing:true, dataSharing:false}
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_customer (tenant_id, customer_uid),
  UNIQUE KEY uk_tenant_external (tenant_id, external_id),
  INDEX idx_tenant_tier (tenant_id, tier_id),
  INDEX idx_email_hash (tenant_id, email_hash),
  INDEX idx_phone_hash (tenant_id, phone_hash),
  INDEX idx_last_activity (tenant_id, last_activity_at),
  INDEX idx_inactivity (tenant_id, inactivity_flag),
  FULLTEXT ft_search (customer_uid)   -- extend with decrypted names in app search
) ENGINE=InnoDB;

-- Immutability guard for anonymised profiles
CREATE TRIGGER trg_customer_no_reenable BEFORE UPDATE ON customer_profiles
FOR EACH ROW BEGIN
  IF OLD.status = 'ANONYMISED' AND NEW.status != 'ANONYMISED' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot re-enable anonymised profile';
  END IF;
END; |
| --- |
| CREATE TABLE customer_identity_links (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  customer_uid   VARCHAR(128) NOT NULL,
  link_type      ENUM('EMAIL','PHONE','WALLET_ID','POS_CARD','LOYALTY_ID','EXTERNAL_CRM') NOT NULL,
  link_value     VARCHAR(512) NOT NULL,   -- encrypted hash for PII types
  is_primary     TINYINT(1) DEFAULT 0,
  verified_at    DATETIME(6),
  created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_link (tenant_id, link_type, link_value),
  INDEX idx_customer (tenant_id, customer_uid)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE points_ledger (
  id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id           VARCHAR(64) NOT NULL,
  customer_uid        VARCHAR(128) NOT NULL,
  idempotency_key     VARCHAR(128) NOT NULL,  -- SHA-256(tenantId+eventId+ruleId)
  entry_type          ENUM('CREDIT','DEBIT','EXPIRE','ADJUST','COALITION_CREDIT',
                           'COALITION_DEBIT','REVERSAL','WELCOME_BONUS','REFERRAL_BONUS',
                           'MANUAL_CREDIT','MANUAL_DEBIT','CAMPAIGN_BONUS') NOT NULL,
  points              DECIMAL(18,4) NOT NULL CHECK (points > 0),
  source_rule_id      VARCHAR(128),
  source_event_id     VARCHAR(128),
  source_campaign_id  VARCHAR(128),
  source_referral_id  VARCHAR(128),
  coalition_partner_id VARCHAR(64),
  expires_at          DATETIME(6),
  reason_code         VARCHAR(128),   -- required for MANUAL_* entries
  approved_by         VARCHAR(128),   -- required for MANUAL_* entries (maker-checker)
  metadata            JSON,
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_idem (idempotency_key),
  INDEX idx_balance (tenant_id, customer_uid, entry_type, expires_at),
  INDEX idx_expiry  (tenant_id, expires_at),
  INDEX idx_event   (tenant_id, source_event_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED;

-- IMMUTABILITY: no UPDATE or DELETE ever allowed on ledger
CREATE TRIGGER trg_ledger_no_update BEFORE UPDATE ON points_ledger
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ledger rows are immutable';
CREATE TRIGGER trg_ledger_no_delete BEFORE DELETE ON points_ledger
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ledger rows cannot be deleted'; |
| --- |
| CREATE TABLE customer_balance_snapshot (
  tenant_id          VARCHAR(64) NOT NULL,
  customer_uid       VARCHAR(128) NOT NULL,
  available_points   DECIMAL(18,4) NOT NULL DEFAULT 0,
  lifetime_earned    DECIMAL(18,4) NOT NULL DEFAULT 0,
  lifetime_redeemed  DECIMAL(18,4) NOT NULL DEFAULT 0,
  lifetime_expired   DECIMAL(18,4) NOT NULL DEFAULT 0,
  last_reconciled_at DATETIME(6),
  updated_at         DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (tenant_id, customer_uid)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE loyalty_rules (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  rule_uid        VARCHAR(128) NOT NULL,
  tenant_id       VARCHAR(64) NOT NULL,
  name            VARCHAR(255) NOT NULL,
  description     TEXT,
  rule_type       ENUM('ACCRUAL','MULTIPLIER','ELIGIBILITY','CAP','EXCLUSION','PRIORITY','COALITION') NOT NULL,
  priority        INT NOT NULL DEFAULT 0,
  status          ENUM('DRAFT','PENDING_APPROVAL','ACTIVE','PAUSED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
  execution_mode  ENUM('FIRST_MATCH','ALL_MATCHING') NOT NULL DEFAULT 'ALL_MATCHING',
  trigger_event   VARCHAR(128) NOT NULL,
  conditions      JSON NOT NULL,
  actions         JSON NOT NULL,
  mutual_excl_tag VARCHAR(64),
  valid_from      DATETIME(6),
  valid_until     DATETIME(6),
  version         INT NOT NULL DEFAULT 1,
  created_by      VARCHAR(128) NOT NULL,
  approved_by     VARCHAR(128),
  approved_at     DATETIME(6),
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_rule (tenant_id, rule_uid),
  INDEX idx_active_rules (tenant_id, status, priority, trigger_event)
) ENGINE=InnoDB;

-- GAP FIX #3: Rule versioning for rollback within 24 hours
CREATE TABLE loyalty_rules_history (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  rule_uid      VARCHAR(128) NOT NULL,
  tenant_id     VARCHAR(64) NOT NULL,
  version       INT NOT NULL,
  snapshot      JSON NOT NULL,   -- full rule row serialized to JSON
  changed_by    VARCHAR(128) NOT NULL,
  change_action ENUM('CREATED','UPDATED','STATUS_CHANGED','ROLLED_BACK') NOT NULL,
  change_reason VARCHAR(512),
  changed_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_rule_history (tenant_id, rule_uid, version DESC)
) ENGINE=InnoDB;

-- Rule simulation results (Gap Fix #1)
CREATE TABLE rule_simulation_results (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  simulation_uid    VARCHAR(128) NOT NULL UNIQUE,
  tenant_id         VARCHAR(64) NOT NULL,
  rule_snapshot     JSON NOT NULL,
  date_range_from   DATETIME(6) NOT NULL,
  date_range_to     DATETIME(6) NOT NULL,
  events_evaluated  INT NOT NULL DEFAULT 0,
  customers_affected INT NOT NULL DEFAULT 0,
  est_points_total  DECIMAL(18,4) NOT NULL DEFAULT 0,
  est_cost_inr      DECIMAL(18,2) NOT NULL DEFAULT 0,
  sample_results    JSON,   -- array of {customerId, estimatedPoints, matchedConditions}
  status            ENUM('RUNNING','COMPLETED','FAILED') DEFAULT 'RUNNING',
  created_by        VARCHAR(128) NOT NULL,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completed_at      DATETIME(6)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE tier_definitions (
  id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id             VARCHAR(64) NOT NULL,
  tier_uid              VARCHAR(128) NOT NULL,
  name                  VARCHAR(128) NOT NULL,
  rank_order            INT NOT NULL,  -- 1=lowest, higher=better
  entry_threshold       DECIMAL(18,4) NOT NULL,
  maintenance_threshold DECIMAL(18,4) NOT NULL,
  threshold_type        ENUM('LIFETIME_POINTS','ROLLING_12M_SPEND','TRANSACTION_COUNT') NOT NULL,
  points_multiplier     DECIMAL(6,3) NOT NULL DEFAULT 1.000,
  grace_period_days     INT NOT NULL DEFAULT 90,
  downgrade_warning_days INT NOT NULL DEFAULT 60,
  benefits              JSON,  -- {exclusiveRewards:true, dedicatedSupport:false, ...}
  is_invite_only        TINYINT(1) DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_tier (tenant_id, tier_uid)
) ENGINE=InnoDB;

CREATE TABLE tier_evaluation_log (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  customer_uid    VARCHAR(128) NOT NULL,
  previous_tier   VARCHAR(128),
  new_tier        VARCHAR(128),
  trigger_reason  ENUM('EVENT','SCHEDULED_REVIEW','MANUAL','GRACE_EXPIRY') NOT NULL,
  qualifying_value DECIMAL(18,4),
  evaluated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_customer_eval (tenant_id, customer_uid)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE campaigns (
  id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id           VARCHAR(64) NOT NULL,
  campaign_uid        VARCHAR(128) NOT NULL,
  name                VARCHAR(255) NOT NULL,
  campaign_type       ENUM('SPEND_EARN','CASHBACK','DISCOUNT','MERCHANT_FUNDED','TIME_BOUND','REFERRAL') NOT NULL,
  status              ENUM('DRAFT','PENDING_APPROVAL','ACTIVE','PAUSED','EXHAUSTED','ENDED') NOT NULL DEFAULT 'DRAFT',
  budget_total        DECIMAL(18,2) NOT NULL,
  budget_consumed     DECIMAL(18,2) NOT NULL DEFAULT 0.00,  -- atomically decremented
  alert_threshold_pct DECIMAL(5,2) NOT NULL DEFAULT 80.00,  -- alert at 80%
  target_segment      JSON,   -- segment criteria for eligibility
  offer_config        JSON,   -- multiplier, cashback%, discount amount, etc.
  eligibility_rules   JSON,
  priority            INT NOT NULL DEFAULT 0,
  is_stackable        TINYINT(1) NOT NULL DEFAULT 0,
  max_participations  INT,    -- NULL = unlimited
  max_per_customer    INT NOT NULL DEFAULT 1,
  merchant_id         VARCHAR(128),   -- NULL for platform campaigns
  valid_from          DATETIME(6) NOT NULL,
  valid_until         DATETIME(6) NOT NULL,
  created_by          VARCHAR(128) NOT NULL,
  approved_by         VARCHAR(128),
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_campaign (tenant_id, campaign_uid),
  INDEX idx_active_campaigns (tenant_id, status, valid_from, valid_until)
) ENGINE=InnoDB;

CREATE TABLE campaign_participations (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  campaign_uid    VARCHAR(128) NOT NULL,
  customer_uid    VARCHAR(128) NOT NULL,
  event_id        VARCHAR(128) NOT NULL,
  points_awarded  DECIMAL(18,4),
  cashback_amount DECIMAL(18,2),
  participated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_participation (tenant_id, campaign_uid, customer_uid, event_id),
  INDEX idx_customer_campaign (tenant_id, customer_uid, campaign_uid)
) ENGINE=InnoDB;

CREATE TABLE coupons (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  coupon_uid     VARCHAR(128) NOT NULL,
  coupon_code    VARCHAR(64) NOT NULL,
  coupon_type    ENUM('FIXED_DISCOUNT','PCT_DISCOUNT','FREE_ITEM','CASHBACK','POINTS_BONUS') NOT NULL,
  discount_value DECIMAL(10,2),
  discount_pct   DECIMAL(5,2),
  status         ENUM('ACTIVE','REDEEMED','EXPIRED','REVOKED') NOT NULL DEFAULT 'ACTIVE',
  usage_type     ENUM('SINGLE_USE','MULTI_USE') NOT NULL DEFAULT 'SINGLE_USE',
  max_redemptions INT NOT NULL DEFAULT 1,
  redemption_count INT NOT NULL DEFAULT 0,
  is_stackable   TINYINT(1) NOT NULL DEFAULT 0,
  target_customer_uid VARCHAR(128),   -- NULL = public
  valid_from     DATETIME(6),
  valid_until    DATETIME(6) NOT NULL,
  campaign_uid   VARCHAR(128),
  created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_code (tenant_id, coupon_code),
  INDEX idx_customer (tenant_id, target_customer_uid)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE referral_programmes (
  id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id          VARCHAR(64) NOT NULL,
  programme_uid      VARCHAR(128) NOT NULL,
  name               VARCHAR(255) NOT NULL,
  referrer_reward    JSON NOT NULL,  -- {type:'POINTS', amount:100, milestoneEvent:'FIRST_TRANSACTION'}
  referee_reward     JSON NOT NULL,  -- {type:'POINTS', amount:50, milestone:'SIGNUP'}
  multi_stage_config JSON,           -- [{stage:1, event:'SIGNUP', reward:50}, {stage:2, event:'TXN3', reward:200}]
  max_referrals_per_customer INT NOT NULL DEFAULT 50,
  status             ENUM('ACTIVE','PAUSED','ENDED') NOT NULL DEFAULT 'ACTIVE',
  valid_from         DATETIME(6),
  valid_until        DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE referrals (
  id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id          VARCHAR(64) NOT NULL,
  referral_uid       VARCHAR(128) NOT NULL,
  programme_uid      VARCHAR(128) NOT NULL,
  referrer_uid       VARCHAR(128) NOT NULL,
  referee_uid        VARCHAR(128),   -- populated after referee signs up
  referral_code      VARCHAR(64) NOT NULL,
  status             ENUM('PENDING','SIGNED_UP','MILESTONE_MET','REWARDED','FRAUD_FLAGGED','REJECTED') NOT NULL DEFAULT 'PENDING',
  fraud_check_result JSON,
  current_stage      INT NOT NULL DEFAULT 0,
  completed_stages   JSON,   -- [{stage:1, completedAt:'...', rewardLedgerEntryId:123}]
  created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_referral (tenant_id, referral_uid),
  UNIQUE KEY uk_code (tenant_id, referral_code)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE merchants (
  id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id             VARCHAR(64) NOT NULL,
  merchant_uid          VARCHAR(128) NOT NULL,
  legal_name            VARCHAR(255) NOT NULL,
  category              VARCHAR(128),
  contact_email_enc     VARBINARY(512),
  bank_details_vault_ref VARCHAR(255),  -- Vault path to encrypted bank details
  tax_id_enc            VARBINARY(256),
  onboarding_stage      ENUM('REGISTRATION','AGREEMENT','CONFIGURATION','INTEGRATION','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'REGISTRATION',
  earn_rate_multiplier  DECIMAL(6,3) NOT NULL DEFAULT 1.000,
  settlement_cycle      ENUM('DAILY','WEEKLY','FORTNIGHTLY','MONTHLY') NOT NULL DEFAULT 'MONTHLY',
  commission_config     JSON,
  api_credentials_vault_ref VARCHAR(255),
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_merchant (tenant_id, merchant_uid)
) ENGINE=InnoDB;

CREATE TABLE merchant_settlement_cycles (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  merchant_uid      VARCHAR(128) NOT NULL,
  period_start      DATE NOT NULL,
  period_end        DATE NOT NULL,
  total_points_redeemed DECIMAL(18,4) NOT NULL DEFAULT 0,
  monetary_value    DECIMAL(18,2) NOT NULL DEFAULT 0,
  status            ENUM('PENDING','UNDER_DISPUTE','APPROVED','PAID') NOT NULL DEFAULT 'PENDING',
  dispute_reason    TEXT,
  resolved_at       DATETIME(6),
  report_s3_path    VARCHAR(512),
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

-- External partner rewards (Gap Fix #2: pending state for unavailable partner APIs)
CREATE TABLE partner_redemption_requests (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  request_uid       VARCHAR(128) NOT NULL UNIQUE,
  customer_uid      VARCHAR(128) NOT NULL,
  partner_id        VARCHAR(128) NOT NULL,   -- e.g. 'AIRLINE_X', 'HOTEL_Y'
  points_to_deduct  DECIMAL(18,4) NOT NULL,
  partner_units     DECIMAL(18,4) NOT NULL,  -- e.g. airline miles = points * conversion_rate
  conversion_rate   DECIMAL(10,6) NOT NULL,
  status            ENUM('RESERVED','PENDING_PARTNER','COMPLETED','FAILED','REVERSED') NOT NULL DEFAULT 'RESERVED',
  partner_confirmation_ref VARCHAR(255),
  retry_count       INT NOT NULL DEFAULT 0,
  next_retry_at     DATETIME(6),
  ledger_entry_id   BIGINT UNSIGNED,   -- points_ledger.id of the DEBIT entry (inserted on COMPLETED)
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE missions (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  mission_uid     VARCHAR(128) NOT NULL,
  name            VARCHAR(255) NOT NULL,
  criteria        JSON NOT NULL,   -- steps: [{eventType, count, timeWindowDays}]
  reward_config   JSON NOT NULL,
  status          ENUM('ACTIVE','PAUSED','ENDED') NOT NULL DEFAULT 'ACTIVE',
  valid_until     DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE customer_mission_progress (
  tenant_id       VARCHAR(64) NOT NULL,
  customer_uid    VARCHAR(128) NOT NULL,
  mission_uid     VARCHAR(128) NOT NULL,
  progress        JSON NOT NULL,   -- {step1:{count:3,required:5}, step2:{...}}
  status          ENUM('IN_PROGRESS','COMPLETED','EXPIRED') DEFAULT 'IN_PROGRESS',
  completed_at    DATETIME(6),
  PRIMARY KEY (tenant_id, customer_uid, mission_uid)
) ENGINE=InnoDB;

CREATE TABLE badges (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  badge_uid       VARCHAR(128) NOT NULL,
  name            VARCHAR(255) NOT NULL,
  criteria_event  VARCHAR(128),
  icon_s3_path    VARCHAR(512)
) ENGINE=InnoDB;

CREATE TABLE customer_badges (
  tenant_id       VARCHAR(64) NOT NULL,
  customer_uid    VARCHAR(128) NOT NULL,
  badge_uid       VARCHAR(128) NOT NULL,
  granted_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (tenant_id, customer_uid, badge_uid)  -- idempotent upsert via INSERT IGNORE
) ENGINE=InnoDB;

CREATE TABLE leaderboard_snapshots (
  tenant_id       VARCHAR(64) NOT NULL,
  leaderboard_type ENUM('GLOBAL','MERCHANT','REGION','TIER') NOT NULL,
  scope_id        VARCHAR(128),   -- merchant_uid or region code
  customer_uid    VARCHAR(128) NOT NULL,
  score           DECIMAL(18,4) NOT NULL,
  rank_position   INT NOT NULL,
  snapshot_date   DATE NOT NULL,
  PRIMARY KEY (tenant_id, leaderboard_type, scope_id, customer_uid, snapshot_date)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE notification_templates (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  template_uid    VARCHAR(128) NOT NULL,
  event_type      VARCHAR(128) NOT NULL,
  channel         ENUM('PUSH','SMS','EMAIL','IN_APP') NOT NULL,
  subject_template VARCHAR(512),
  body_template   TEXT NOT NULL,   -- supports {customerName}, {points}, {expiryDate} placeholders
  is_transactional TINYINT(1) NOT NULL DEFAULT 1,  -- transactional = sent even to opted-out
  language        VARCHAR(10) NOT NULL DEFAULT 'en',
  UNIQUE KEY uk_template (tenant_id, event_type, channel, language)
) ENGINE=InnoDB;

CREATE TABLE notification_log (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  customer_uid    VARCHAR(128) NOT NULL,
  channel         ENUM('PUSH','SMS','EMAIL','IN_APP') NOT NULL,
  event_type      VARCHAR(128) NOT NULL,
  status          ENUM('QUEUED','SENT','DELIVERED','FAILED','OPTED_OUT') NOT NULL DEFAULT 'QUEUED',
  provider_ref    VARCHAR(255),
  sent_at         DATETIME(6),
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_customer_notifs (tenant_id, customer_uid, created_at)
) ENGINE=InnoDB;

CREATE TABLE webhook_subscriptions (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  endpoint_url    VARCHAR(2048) NOT NULL,
  secret_vault_ref VARCHAR(255) NOT NULL,
  events_subscribed JSON NOT NULL,   -- ['POINTS_EARNED','TIER_UPGRADED',...]
  is_active       TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE webhook_delivery_log (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  subscription_id BIGINT UNSIGNED NOT NULL,
  event_type      VARCHAR(128) NOT NULL,
  payload         JSON NOT NULL,
  status          ENUM('PENDING','DELIVERED','FAILED','DLQ') NOT NULL DEFAULT 'PENDING',
  attempt_count   INT NOT NULL DEFAULT 0,
  next_retry_at   DATETIME(6),
  last_error      TEXT,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE audit_log (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  actor_id        VARCHAR(128) NOT NULL,
  actor_role      VARCHAR(64) NOT NULL,
  action          VARCHAR(128) NOT NULL,
  entity_type     VARCHAR(64) NOT NULL,
  entity_id       VARCHAR(128) NOT NULL,
  before_state    JSON,
  after_state     JSON,
  ip_address      VARCHAR(45),
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_entity (tenant_id, entity_type, entity_id),
  INDEX idx_actor  (tenant_id, actor_id, created_at)
) ENGINE=InnoDB;

-- Immutable audit log
CREATE TRIGGER trg_audit_no_update BEFORE UPDATE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Audit log is immutable';
CREATE TRIGGER trg_audit_no_delete BEFORE DELETE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Audit log is immutable'; |
| --- |
| CREATE TABLE approval_requests (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  request_uid       VARCHAR(128) NOT NULL UNIQUE,
  entity_type       ENUM('RULE','CAMPAIGN','MANUAL_ADJUSTMENT','MERCHANT_CONFIG','TIER_CONFIG','COALITION') NOT NULL,
  entity_id         VARCHAR(128) NOT NULL,
  action_requested  ENUM('CREATE','UPDATE','ACTIVATE','DEACTIVATE','DELETE','ROLLBACK') NOT NULL,
  payload           JSON NOT NULL,    -- full proposed state to be approved
  requested_by      VARCHAR(128) NOT NULL,
  status            ENUM('PENDING','APPROVED','REJECTED','EXPIRED') NOT NULL DEFAULT 'PENDING',
  reviewed_by       VARCHAR(128),
  review_comment    TEXT,
  reviewed_at       DATETIME(6),
  expires_at        DATETIME(6),      -- auto-expire unreviewed requests after 48h
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  -- Constraint: reviewer cannot be the same as requester
  CHECK (reviewed_by IS NULL OR reviewed_by != requested_by)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE fraud_alerts (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  alert_uid         VARCHAR(128) NOT NULL UNIQUE,
  customer_uid      VARCHAR(128),
  merchant_uid      VARCHAR(128),
  fraud_type        ENUM('SELF_REFERRAL','VELOCITY_ABUSE','ACCOUNT_TAKEOVER','COUPON_STACKING',
                         'MERCHANT_FRAUD','PROMO_EXPLOITATION','CLUSTER_DETECTION') NOT NULL,
  severity          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  status            ENUM('OPEN','UNDER_REVIEW','RESOLVED_FRAUD','RESOLVED_FALSE_POSITIVE') NOT NULL DEFAULT 'OPEN',
  trigger_details   JSON NOT NULL,
  investigation_notes TEXT,
  resolved_by       VARCHAR(128),
  resolved_at       DATETIME(6),
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE fraud_blocked_events (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  event_id        VARCHAR(128) NOT NULL UNIQUE,
  customer_uid    VARCHAR(128) NOT NULL,
  reason          VARCHAR(512) NOT NULL,
  fraud_alert_id  VARCHAR(128),
  status          ENUM('BLOCKED','RELEASED','PERMANENTLY_BLOCKED') NOT NULL DEFAULT 'BLOCKED',
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE device_blacklist (
  tenant_id         VARCHAR(64) NOT NULL,
  device_fingerprint VARCHAR(255) NOT NULL,
  reason            VARCHAR(512),
  blacklisted_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (tenant_id, device_fingerprint)
) ENGINE=InnoDB; |
| --- |
| CREATE TABLE coalition_partnerships (
  id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  partnership_uid      VARCHAR(128) NOT NULL UNIQUE,
  tenant_a_id          VARCHAR(64) NOT NULL,
  tenant_b_id          VARCHAR(64) NOT NULL,
  status               ENUM('PENDING_APPROVAL','ACTIVE','SUSPENDED','TERMINATED') NOT NULL DEFAULT 'PENDING_APPROVAL',
  a_to_b_rate          DECIMAL(10,6) NOT NULL,
  b_to_a_rate          DECIMAL(10,6) NOT NULL,
  earn_shared          TINYINT(1) NOT NULL DEFAULT 1,
  redeem_shared        TINYINT(1) NOT NULL DEFAULT 1,
  max_redeem_pct       DECIMAL(5,2) NOT NULL DEFAULT 100.00,
  settlement_cycle     ENUM('DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'MONTHLY',
  approved_by_a        VARCHAR(128),
  approved_by_b        VARCHAR(128),
  UNIQUE KEY uk_partners (tenant_a_id, tenant_b_id)
) ENGINE=InnoDB;

CREATE TABLE coalition_customer_links (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  partnership_uid   VARCHAR(128) NOT NULL,
  tenant_a_id       VARCHAR(64) NOT NULL,
  customer_uid_a    VARCHAR(128) NOT NULL,
  tenant_b_id       VARCHAR(64) NOT NULL,
  customer_uid_b    VARCHAR(128),
  link_status       ENUM('LINKED','PENDING_VERIFICATION','UNLINKED') NOT NULL DEFAULT 'PENDING_VERIFICATION',
  linked_at         DATETIME(6),
  UNIQUE KEY uk_link_a (partnership_uid, customer_uid_a)
) ENGINE=InnoDB;

CREATE TABLE coalition_settlement_records (
  id                       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  settlement_uid           VARCHAR(128) NOT NULL UNIQUE,
  partnership_uid          VARCHAR(128) NOT NULL,
  period_start             DATE NOT NULL,
  period_end               DATE NOT NULL,
  debtor_tenant_id         VARCHAR(64) NOT NULL,
  creditor_tenant_id       VARCHAR(64) NOT NULL,
  total_points_transferred DECIMAL(18,4) NOT NULL,
  monetary_value           DECIMAL(18,2) NOT NULL,
  currency                 VARCHAR(3) NOT NULL DEFAULT 'INR',
  status                   ENUM('PENDING','APPROVED','PAID','DISPUTED') NOT NULL DEFAULT 'PENDING',
  created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB; |
| --- |
| Why These Gaps Matter
These 5 items were identified in the architecture review as undesigned or under-designed. They are now fully specified below. Building without these designs will cause production issues: financial inaccuracy, data corruption, or BRD non-compliance. |
| --- |
| POST /v1/{tenantId}/rules/{ruleUid}/simulate
Auth: JWT (ADMIN or PROGRAM_MANAGER role only)
Request body:
{
  "dateRangeFrom": "2025-01-01T00:00:00Z",
  "dateRangeTo":   "2025-03-31T23:59:59Z",
  "maxEventsToSample": 10000   // ClickHouse LIMIT for cost control
}
Response: 202 Accepted  { simulationUid: 'sim_abc123', status: 'RUNNING' }

GET /v1/{tenantId}/rules/{ruleUid}/simulate/{simulationUid}
Response when COMPLETED:
{
  "status": "COMPLETED",
  "eventsEvaluated": 8743,
  "customersAffected": 1205,
  "estimatedPointsTotal": 452000.0000,
  "estimatedCostInr": 4520.00,
  "sampleResults": [{"customerUid":"C1","estimatedPoints":450.0,...}]
} |
| --- |
| // RuleSimulationService.java
public SimulationResult simulate(String tenantId, String ruleUid, SimulationRequest req) {
    // 1. Load DRAFT rule from MySQL by (tenantId, ruleUid)
    LoyaltyRule rule = ruleRepository.findByTenantAndUid(tenantId, ruleUid);
    Assert.state(rule.getStatus() == DRAFT, 'Can only simulate DRAFT rules');

    // 2. Fetch sample events from ClickHouse (never MySQL)
    List<EnrichedEvent> events = clickHouseEventRepo.fetchSample(
        tenantId, rule.getTriggerEvent(), req.getDateFrom(), req.getDateTo(), req.getMaxEvents());

    // 3. For each event, run SpEL eval in DRY_RUN context
    long affected = 0; BigDecimal totalPts = BigDecimal.ZERO;
    for (EnrichedEvent ev : events) {
        EvaluationContext ctx = buildContext(ev, DRY_RUN_FLAG);
        if (spelEvaluator.evaluate(rule.getConditions(), ctx)) {
            BigDecimal pts = spelEvaluator.computePoints(rule.getActions(), ctx);
            totalPts = totalPts.add(pts); affected++;
        }
    }
    // 4. Save to rule_simulation_results — NEVER writes to points_ledger
    return saveSimulationResult(tenantId, ruleUid, affected, totalPts);
} |
| --- |
| State: RESERVED
  → When customer initiates partner redemption
  → Points NOT yet deducted from ledger (balance is 'soft-reserved' in Redis)
  → Redis: SETNX reserve:{tenantId}:{customerUid}:{requestUid} {points} EX 86400

State: PENDING_PARTNER (transition from RESERVED when partner API call fails)
  → Partner API returned 5xx or timeout
  → Spring Batch job picks up all rows WHERE status='PENDING_PARTNER' AND next_retry_at <= NOW()
  → Max retry_count = 8 (covers ~24 hours with backoff)

State: COMPLETED (transition from RESERVED or PENDING_PARTNER on partner API success)
  → BEGIN MySQL TRANSACTION
  →   INSERT INTO points_ledger (entry_type='DEBIT', ...) -- actual deduction
  →   UPDATE partner_redemption_requests SET status='COMPLETED', ledger_entry_id=...
  → COMMIT
  → Delete Redis reservation key
  → Publish PartnerRedemptionCompleted Kafka event

State: FAILED (after max retries exhausted)
  → Release Redis reservation
  → Notify customer: 'Partner redemption unavailable — your points have been returned'
  → Alert operations team via PagerDuty

State: REVERSED (customer cancels or ops team reverses)
  → Release Redis reservation (if RESERVED) or INSERT REVERSAL ledger entry (if COMPLETED) |
| --- |
| CREATE TABLE partner_reward_catalogue (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  partner_id        VARCHAR(128) NOT NULL,
  partner_name      VARCHAR(255) NOT NULL,
  reward_type       VARCHAR(128) NOT NULL,  -- 'AIRLINE_MILES', 'HOTEL_POINTS', 'GIFT_CARD'
  conversion_rate   DECIMAL(10,6) NOT NULL, -- 100 loyalty pts = 1 partner unit
  min_redemption    DECIMAL(10,4) NOT NULL,
  api_endpoint_vault_ref VARCHAR(255) NOT NULL,
  is_active         TINYINT(1) NOT NULL DEFAULT 1,
  updated_at        DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB; |
| --- |
| // RuleVersioningAspect.java
@Aspect @Component
public class RuleVersioningAspect {
    @Before("execution(* RuleService.update*(..)) || execution(* RuleService.changeStatus(..))")
    public void captureVersion(JoinPoint jp) {
        String tenantId = TenantContext.get();
        LoyaltyRule current = ruleRepo.findByUid(tenantId, extractRuleUid(jp));
        // Save snapshot BEFORE the update happens
        rulesHistoryRepo.save(LoyaltyRulesHistory.builder()
            .ruleUid(current.getRuleUid()).tenantId(tenantId)
            .version(current.getVersion())
            .snapshot(objectMapper.valueToTree(current))
            .changedBy(SecurityContext.currentUserId())
            .changeAction(UPDATED).build());
    }
}

// Rollback API
POST /v1/{tenantId}/rules/{ruleUid}/rollback
Body: { targetVersion: 3 }
Auth: ADMIN only
Action:
  1. SELECT snapshot FROM loyalty_rules_history WHERE rule_uid=? AND version=?
  2. BEGIN TRANSACTION
  3.   UPDATE loyalty_rules SET ...snapshot fields..., version = MAX(version)+1
  4.   INSERT INTO loyalty_rules_history (changeAction='ROLLED_BACK')
  5. COMMIT
  6. Publish platform.config.updates Kafka event → all Rule Engine pods reload within 10s |
| --- |
| // RewardEngine.java — inactivity check before every points award
private void checkInactivityFlag(String tenantId, String customerUid, String eventId) {
    CustomerProfile profile = profileRepo.findByUid(tenantId, customerUid);  // MySQL read
    if (profile.isInactivityFlag()) {
        // Publish notification event — reward still proceeds
        kafkaTemplate.send('platform.notifications', CustomerReactivationNotification.of(
            tenantId, customerUid, eventId));
        // Reset flag — customer is now active again
        profileRepo.clearInactivityFlag(tenantId, customerUid);
        // Update last_activity_at
        profileRepo.updateLastActivity(tenantId, customerUid, Instant.now());
        auditLog.record(tenantId, 'INACTIVITY_FLAG_CLEARED', customerUid);
    }
} |
| --- |
| // CampaignBudgetService.java
public boolean tryDecrementBudget(String tenantId, String campaignUid, BigDecimal rewardCost) {
    // Single atomic statement — no separate SELECT needed
    // InnoDB row-level lock prevents concurrent overspend
    int rows = jdbcTemplate.update(
        "UPDATE campaigns " +
        "SET budget_consumed = budget_consumed + ?, " +
        "    status = IF(budget_consumed + ? >= budget_total, 'EXHAUSTED', status) " +
        "WHERE tenant_id = ? AND campaign_uid = ? " +
        "  AND status = 'ACTIVE' " +
        "  AND (budget_consumed + ?) <= budget_total",
        rewardCost, rewardCost, tenantId, campaignUid, rewardCost);

    if (rows == 0) return false;  // Campaign exhausted or inactive — reject reward

    // Check if we just crossed the 80% alert threshold
    Campaign c = campaignRepo.findByUid(tenantId, campaignUid);
    if (c.getBudgetConsumed().divide(c.getBudgetTotal()) 
           .compareTo(c.getAlertThresholdPct().divide(HUNDRED)) >= 0) {
        kafkaTemplate.send('platform.campaign.alerts', CampaignBudgetAlert.of(c));
    }
    return true;
} |
| --- |
| Gap Fix Summary
All 5 gaps are now fully specified. Each has: MySQL schema, Spring Boot service pattern, API endpoint, and test strategy. No BRD requirement is left unaddressed. |
| --- |
| Two Modes Agreed with Client
MODE A (ID_ONLY): Client sends only their own customer identifier. Platform creates a stub profile.
MODE B (FULL_PROFILE): Client sends full customer PII details. Platform creates/updates complete profile.
Both modes: same event processing, same rule evaluation, same reward issuance. Mode only affects profile completeness. |
| --- |
| POST /v1/{tenantId}/events
Content-Type: application/json
X-LoyaltyOS-Signature: hmac-sha256={timestamp}.{signature}
X-API-Key: {tenantApiKey}

// MODE A — ID Only (minimal)
{
  "externalId": "CLIENT_CUST_99123",
  "eventType": "PURCHASE",
  "amount": 1500.00,
  "currency": "INR",
  "transactionId": "TXN_XYZ_9876",
  "timestamp": "2025-06-15T10:30:00Z",
  "metadata": { "channel": "mobile_app", "merchantId": "MCH_001" }
}

// MODE B — Full Profile
{
  "externalId": "CLIENT_CUST_99123",
  "customer": {
    "firstName": "Priya",
    "lastName": "Sharma",
    "email": "priya@example.com",
    "phone": "+919999999999",
    "dateOfBirth": "1990-04-15",
    "customAttributes": { "preferredLanguage": "hi", "city": "Mumbai" }
  },
  "eventType": "PURCHASE",
  "amount": 1500.00,
  "currency": "INR",
  "transactionId": "TXN_XYZ_9876",
  "timestamp": "2025-06-15T10:30:00Z"
}

Response: HTTP 202 Accepted
{ "ingestionId": "ing_uuid_v7", "customerUid": "cust_platform_uuid", "status": "ACCEPTED" } |
| --- |
| // CustomerProfileService.java
public String upsertFromEvent(String tenantId, IngestEventRequest req) {

    // 1. Look up by external_id (indexed unique key)
    Optional<CustomerProfile> existing =
        profileRepo.findByTenantAndExternalId(tenantId, req.getExternalId());

    if (existing.isEmpty()) {
        // 2a. New customer — create profile
        CustomerProfile profile = CustomerProfile.builder()
            .tenantId(tenantId)
            .customerUid(UUIDv7.generate())  // platform internal ID
            .externalId(req.getExternalId())
            .ingestionMode(req.hasCustomerBlock() ? FULL_PROFILE : ID_ONLY)
            .lastActivityAt(Instant.now())
            .build();

        if (req.hasCustomerBlock()) {
            CustomerBlock c = req.getCustomer();
            // PII encrypted with tenant DEK before storage
            profile.setFirstNameEnc(piiEncryptor.encrypt(tenantId, c.getFirstName()));
            profile.setLastNameEnc(piiEncryptor.encrypt(tenantId, c.getLastName()));
            profile.setEmailEnc(piiEncryptor.encrypt(tenantId, c.getEmail()));
            profile.setPhoneEnc(piiEncryptor.encrypt(tenantId, c.getPhone()));
            profile.setEmailHash(sha256Hex(c.getEmail().toLowerCase().trim()));
            profile.setPhoneHash(sha256Hex(normalizePhone(c.getPhone())));
        }
        profileRepo.save(profile);
        return profile.getCustomerUid();

    } else {
        // 2b. Existing profile — update if full profile received
        CustomerProfile profile = existing.get();
        if (req.hasCustomerBlock() && profile.getIngestionMode() == ID_ONLY) {
            // Upgrade stub to full profile
            CustomerBlock c = req.getCustomer();
            profile.setFirstNameEnc(piiEncryptor.encrypt(tenantId, c.getFirstName()));
            // ... update all PII fields ...
            profile.setIngestionMode(FULL_PROFILE);
        }
        profile.setLastActivityAt(Instant.now());
        profile.setInactivityFlag(false);  // Reset on any activity
        profileRepo.save(profile);
        return profile.getCustomerUid();
    }
} |
| --- |
| Coalition Concept
Two tenants (e.g. Tenant A = Retail Chain, Tenant B = Hotel Chain) formally partner. Their customers can earn points on the partner's platform and redeem points across both. MySQL ACID ensures no points are lost or double-counted during transfer. |
| --- |
| Stage | Who Does It | API | MySQL Change |
| --- | --- | --- | --- |
| 1. Initiate Partnership | Tenant A admin sends request to Tenant B | POST /v1/{tenantId}/coalition/requests | INSERT coalition_partnerships status=PENDING_APPROVAL |
| 2. Tenant B Approves | Tenant B admin accepts | PUT /v1/{tenantId}/coalition/{partnershipUid}/approve | UPDATE status=ACTIVE, approved_by_b set |
| 3. Customer Links Account | End customer in Tenant A links to Tenant B account | POST /v1/{tenantId}/coalition/{partnershipUid}/link | INSERT coalition_customer_links status=LINKED |
| 4. Cross-Platform Earn | Customer transacts on Tenant B — earns points in Tenant A | Automatic via event enrichment | INSERT points_ledger COALITION_CREDIT for Tenant A customer |
| 5. Cross-Platform Redeem | Customer redeems Tenant A points on Tenant B | POST /v1/{tenantId}/coalition/{partnershipUid}/transfer | MySQL ACID: COALITION_DEBIT + COALITION_CREDIT in one txn |
| 6. Settlement | Monthly batch — Tenant B owes Tenant A for redemptions | Scheduled job | INSERT coalition_settlement_records |
| // CoalitionTransferService.java — the most critical method in the coalition module
@Transactional(isolation = Isolation.SERIALIZABLE)  // MySQL InnoDB serializable
public CoalitionTransferResult transfer(CoalitionTransferRequest req) {
    // 1. Validate partnership is ACTIVE
    CoalitionPartnership p = partnershipRepo.findByUid(req.getPartnershipUid());
    Assert.state(p.getStatus() == ACTIVE, 'Partnership not active');

    // 2. Validate customer link exists and is LINKED
    CoalitionCustomerLink link = linkRepo.findByPartnershipAndCustomerA(
        req.getPartnershipUid(), req.getSourceCustomerUid());
    Assert.notNull(link.getCustomerUidB(), 'Customer not linked to partner platform');

    // 3. Check source balance (Redis L1, then MySQL L2)
    BigDecimal balance = balanceService.getAvailableBalance(
        req.getSourceTenantId(), req.getSourceCustomerUid());
    Assert.isTrue(balance.compareTo(req.getPointsToTransfer()) >= 0, 'Insufficient balance');

    // 4. Compute target points using conversion rate
    BigDecimal targetPoints = req.getPointsToTransfer().multiply(p.getAtoBRate());

    // 5. ACID: DEBIT source + CREDIT target in single MySQL transaction
    String idemKey = sha256(req.getPartnershipUid()+req.getSourceCustomerUid()+req.getRequestUid());
    ledgerRepo.insertCredit(req.getSourceTenantId(), req.getSourceCustomerUid(),
        req.getPointsToTransfer(), COALITION_DEBIT, idemKey+"-debit", req.getPartnershipUid());
    ledgerRepo.insertCredit(req.getDestTenantId(), link.getCustomerUidB(),
        targetPoints, COALITION_CREDIT, idemKey+"-credit", req.getPartnershipUid());

    // 6. Insert settlement record
    settlementRepo.insertTransferRecord(p, req.getPointsToTransfer(),
        req.getPointsToTransfer().multiply(tenantConfig.getPointsCurrencyRate()));

    // Transaction commits here — both inserts or neither
    // 7. Update Redis balances (outside transaction — eventual consistency acceptable)
    balanceService.invalidateCache(req.getSourceTenantId(), req.getSourceCustomerUid());
    balanceService.invalidateCache(req.getDestTenantId(), link.getCustomerUidB());

    return CoalitionTransferResult.success(targetPoints);
} |
| --- |
| Requirement | MySQL | Service | API Endpoint | Roles |
| --- | --- | --- | --- | --- |
| Unified profile with PII | customer_profiles + customer_identity_links | CustomerProfileService | GET /v1/{t}/customers/{uid} | ADMIN, SUPPORT, ANALYST(read) |
| Duplicate detection on enrolment | UNIQUE KEY uk_email_hash, uk_phone_hash | CustomerProfileService.checkDuplicate() | Checked during POST /enrol | Internal only |
| 24-month inactivity flag (Gap Fix #4) | inactivity_flag + last_activity_at columns | InactivityBatchJob (nightly) | Automatic — no API | Batch system |
| Profile visible to customer | customer_profiles | CustomerSelfService | GET /v1/{t}/me | CUSTOMER token |
| Segmentation tags | segmentation_tags JSON column | SegmentationService | PATCH /v1/{t}/customers/{uid}/segments | ADMIN, CAMPAIGN_MANAGER |
| Comm opt-out honoured <24h | communication_prefs JSON + notification_log check | NotificationService | PATCH /v1/{t}/me/preferences | CUSTOMER |
| GDPR erasure | UPDATE status=ANONYMISED + null PII fields | GdprService | POST /v1/{t}/customers/{uid}/erase | ADMIN + compliance approval |
| Requirement | MySQL | Service | API Endpoint | Roles |
| --- | --- | --- | --- | --- |
| Auto-enrol at signup / first txn | customer_profiles INSERT + programme_membership INSERT | EnrolmentService | POST /v1/{t}/enrol | SYSTEM / API_KEY |
| Welcome bonus on enrolment | points_ledger INSERT WELCOME_BONUS (via Kafka reward cmd) | RewardEngine | Triggered by EnrolmentService | Internal |
| Eligibility check before enrol | tenant_config.feature_flags.eligibilityRules | RulesEngine.checkEligibility() | Part of enrol flow | Internal |
| Suspend / remove customer | UPDATE customer_profiles SET status='SUSPENDED' | CustomerProfileService | DELETE /v1/{t}/customers/{uid}/membership | ADMIN |
| Programme start/end dates | campaigns.valid_from / valid_until | ProgrammeService | Checked at every event | Internal |
| Requirement | MySQL | Service | API Endpoint | Roles |
| --- | --- | --- | --- | --- |
| Define tiers with thresholds | tier_definitions table | TierConfigService | POST /v1/{t}/tiers | ADMIN |
| Auto-upgrade on threshold crossed | tier_evaluation_log + customer_profiles.tier_id update | TierEvaluationService (async, <5s) | Triggered by Kafka reward event | Internal |
| 60-day downgrade warning | Nightly batch: check tier maintenance vs rolling 12m spend | TierDowngradeWarningJob | Sends notification via Kafka | Batch |
| Grace period 90 days | customer_profiles.tier_protected_until DATETIME | TierEvaluationService | Set on tier downgrade trigger | Internal |
| Points multiplier per tier | tier_definitions.points_multiplier | RulesEngine — applied at point calc | Applied automatically in reward formula | Internal |
| Manual tier override | UPDATE customer_profiles.tier_id with approval_requests record | TierConfigService | PUT /v1/{t}/customers/{uid}/tier | ADMIN (maker-checker required) |
| Requirement | MySQL / Redis | Service | API Endpoint | Roles |
| --- | --- | --- | --- | --- |
| Real-time points balance | Redis L1: customer_balance:{t}:{uid} | MySQL L2: customer_balance_snapshot | BalanceService | GET /v1/{t}/customers/{uid}/wallet | CUSTOMER, SUPPORT, ADMIN |
| Transaction history (paginated) | points_ledger SELECT with cursor pagination | LedgerService | GET /v1/{t}/customers/{uid}/transactions?page=&size=&type=&from=&to= | CUSTOMER, SUPPORT, ADMIN |
| Expiry schedule | SELECT from points_ledger WHERE expires_at > NOW() ORDER BY expires_at | LedgerService | GET /v1/{t}/customers/{uid}/wallet/expiry-summary | CUSTOMER |
| Negative balance prevented | MySQL CHECK (points > 0) + balance check before DEBIT INSERT | RewardEngine | Automatic | Internal |
| Every txn traceable to source | source_rule_id, source_event_id, source_campaign_id columns on ledger | LedgerService | Returned in transaction history | — |
| Requirement | MySQL / Redis | Service | Kafka Topic | Notes |
| --- | --- | --- | --- | --- |
| Auto-award on qualifying txn | points_ledger INSERT CREDIT | RewardEngine | platform.reward.commands → platform.rewards.issued | Idempotency_key prevents duplicates |
| Tier multiplier applied | tier_definitions.points_multiplier × base formula | RulesEngine (SpEL formula eval) | — | e.g. event.amount * 0.01 * tier.multiplier |
| Daily / monthly earn caps | tenant_config.daily_points_cap + Redis INCR daily counter | RewardEngine.checkCaps() | — | Redis: earn_cap:{t}:{cuid}:{date} INCR, EXPIRE end-of-day |
| Channel-based earn rules | loyalty_rules conditions: event.metadata.channel == 'mobile_app' | RulesEngine | — | Condition field in rule JSON |
| Manual point grant | points_ledger INSERT MANUAL_CREDIT | ManualAdjustmentService | platform.audit.log | Requires approval_requests record (maker-checker) |
| Every point traceable | source_event_id + source_rule_id on every ledger row | — | — | Non-negotiable BRD rule |
| Requirement | MySQL / Redis | Service | API Endpoint | Notes |
| --- | --- | --- | --- | --- |
| Redeem for discount/cashback/voucher | points_ledger INSERT DEBIT + campaign_participations | RedemptionService | POST /v1/{t}/customers/{uid}/redeem | Idempotent via redemption_reference |
| Partial redemption | Any amount <= available_points, min set in tier_definitions | RedemptionService | — | Balance check: Redis L1 → MySQL L2 |
| Balance validated before redeem | Redis DECRBY + InnoDB serializable if Redis miss | RedemptionService | — | Never allow negative balance |
| Confirmation in real-time | platform.rewards.issued Kafka event → notification | NotificationService | — | Push/SMS within 60s |
| POS / merchant redemption | Same API — merchant token in X-Merchant-ID header | RedemptionService | POST /v1/{t}/customers/{uid}/redeem | Merchant API token auth |
| Requirement | MySQL | Service / Job | Notes |
| --- | --- | --- | --- |
| Rolling expiry (per-entry TTL) | points_ledger.expires_at set at INSERT time | ExpiryBatchJob nightly | Batch runs 02:00 UTC — not during peak |
| Fixed-date expiry | Same column — set to tenant-configured campaign end date | ExpiryBatchJob | Supported via campaign_uid link |
| Tier-based extended expiry | tier_definitions: add 6 months for Gold/Platinum | RewardEngine computes expires_at at INSERT | If customer is Gold, expires_at = issued_at + 18 months |
| 60-day expiry warning | Nightly: SELECT points WHERE expires_at BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 60 DAY) | ExpiryNotificationJob | Sends via platform.notifications Kafka topic |
| 7-day final warning | Same job, different threshold | ExpiryNotificationJob | SMS + push (transactional — sent even to opted-out) |
| Breakage accounting | INSERT points_ledger EXPIRE entries in batch + update breakage_log | ExpiryBatchJob | Monthly finance report reads SUM of EXPIRE entries |
| Batch never during peak | Spring Batch scheduled at 02:00 UTC | @Scheduled cron | 02:00 UTC = 07:30 IST — minimal traffic |
| Requirement | MySQL | Service | API / Kafka | Notes |
| --- | --- | --- | --- | --- |
| CRUD rules — no code change | loyalty_rules table | RuleService | POST/GET/PUT/DELETE /v1/{t}/rules | ADMIN, PROGRAM_MANAGER |
| Status lifecycle DRAFT→ACTIVE | loyalty_rules.status ENUM | RuleService.changeStatus() | PUT /v1/{t}/rules/{uid}/status | Requires maker-checker if ACTIVE |
| Propagate activation in ≤10s | loyalty_rules_history INSERT + Kafka config.updates publish | RuleService + Kafka | platform.config.updates | Redis cache invalidation on all Rule Engine pods |
| All condition operators (EQ,GT,IN,BETWEEN etc) | conditions JSON stores AST tree | SpEL evaluator in RuleEngine | — | SpEL handles all operators natively |
| AND/OR/NOT nesting | conditions JSON: {op:'AND', nodes:[...]} | SpEL evaluator | — | Unlimited nesting depth |
| Rule versioning + rollback | loyalty_rules_history (Gap Fix #3) | RuleVersioningAspect + RuleRollbackService | POST /v1/{t}/rules/{uid}/rollback | ADMIN only |
| Simulation tool | rule_simulation_results (Gap Fix #1) | RuleSimulationService | POST /v1/{t}/rules/{uid}/simulate | ADMIN, PROGRAM_MANAGER |
| Conflict resolution: FIRST_MATCH / ALL_MATCHING | loyalty_rules.execution_mode | RuleEngine | — | Configured per rule group |
| Scheduled activation windows | valid_from / valid_until on loyalty_rules | RuleEngine checks at eval time | — | Rules outside window are skipped |
| Per-tenant rule limit | tenant_config.max_active_rules | RuleService.validateRuleLimit() | — | Standard=50, Pro=500, Ent=unlimited |
| LLM rule generation | No MySQL change — LLM API call returns rule JSON | LlmRuleGeneratorService | POST /v1/{t}/rules/generate-from-text | LLM output always DRAFT; never auto-activated |
| Requirement | MySQL | Service | API | Notes |
| --- | --- | --- | --- | --- |
| Campaign CRUD with targeting | campaigns + campaign_participations | CampaignService | POST/GET/PUT /v1/{t}/campaigns | ADMIN, PROGRAM_MANAGER |
| Budget auto-stop | campaigns.budget_consumed atomic decrement (Gap Fix #5) | CampaignBudgetService.tryDecrementBudget() | — | Single UPDATE statement; 0 rows = exhausted |
| 80% budget alert | budget_consumed >= 0.8 * budget_total check after decrement | CampaignBudgetService | platform.campaign.alerts Kafka | Alert to campaign owner + merchant |
| Maker-checker for large campaigns | approval_requests table | ApprovalWorkflowService | POST /v1/{t}/approvals | Required above budget threshold |
| Campaign priority when stacked | campaigns.priority + campaigns.is_stackable | CampaignService.resolveConflicts() | — | BRD 4.8.4 conflict resolution logic |
| Real-time performance tracking | campaign_participations COUNT + budget_consumed | CampaignAnalyticsService | GET /v1/{t}/campaigns/{uid}/stats | ADMIN, ANALYST, MERCHANT |
| Merchant-funded campaigns | campaigns.merchant_id FK to merchants | CampaignService | Merchant portal API | Merchant campaign portal Section 7.14 |
| Requirement | MySQL | Service | API | Notes |
| --- | --- | --- | --- | --- |
| Create coupons (fixed/pct/cashback) | coupons table | CouponService | POST /v1/{t}/coupons | ADMIN, PROGRAM_MANAGER |
| Single-use / multi-use | coupons.usage_type + redemption_count | CouponService | — | UNIQUE KEY on (tenant, code) |
| Auto-invalidate expired | coupons.status='EXPIRED' via nightly batch or at validation time | CouponValidationService | — | Checked at point of use |
| Validate at POS / app | SELECT + status check + redemption_count check | CouponValidationService | POST /v1/{t}/coupons/{code}/validate | Merchant + customer token |
| Stackable/exclusive rules | coupons.is_stackable | CouponValidationService.checkStacking() | — | If non-stackable and another coupon applied: 422 |
| Channel restrictions | coupons metadata JSON: channel restriction | CouponValidationService | — | Checked against X-Channel header |
| Requirement | MySQL | Service | API | Notes |
| --- | --- | --- | --- | --- |
| Unique referral code per customer | referrals.referral_code — UUID-based + tenant prefix | ReferralService | GET /v1/{t}/me/referral-code | Customer token |
| Link referrer → referee on signup | referrals INSERT with referee_uid on signup | ReferralService.linkReferee() | Called by enrolment flow | Fraud checks run first |
| Reward only on milestone | referrals.status state machine: PENDING→SIGNED_UP→MILESTONE_MET→REWARDED | ReferralMilestoneService | Triggered by event evaluations | Multi-stage config supported |
| Multi-stage rewards | referral_programmes.multi_stage_config JSON | ReferralMilestoneService | — | Each stage fires separate reward command |
| Cap referrals per customer | referral_programmes.max_referrals_per_customer | ReferralService.checkCap() | — | COUNT(referrals WHERE referrer_uid=? AND status!=REJECTED) |
| Fraud checks | Device fingerprint + phone_hash + email_hash cross-check on signup | FraudDetectionService | — | Runs before referral is linked |
| Performance dashboard | referrals aggregate queries | ReferralAnalyticsService | GET /v1/{t}/referrals/stats | ADMIN, ANALYST |
| Requirement | MySQL | Service | API | Notes |
| --- | --- | --- | --- | --- |
| Catalogue of redeemable rewards | rewards_catalogue table (see schema below) | RewardsCatalogueService | GET /v1/{t}/catalogue | CUSTOMER (filtered by tier access) |
| Zero-stock = hidden | rewards_catalogue.stock_quantity=0 → status auto-set to HIDDEN | RewardsCatalogueService | — | MySQL trigger on stock update |
| Reserve on redemption (<5s) | rewards_catalogue.stock_quantity atomic decrement | RewardsCatalogueService | — | Same pattern as campaign budget decrement |
| Low-stock alerts | nightly check: stock_quantity < low_stock_threshold | InventoryAlertJob | platform.inventory.alerts Kafka | Alert to admin + email |
| Partner rewards (Gap Fix #2) | partner_reward_catalogue + partner_redemption_requests | PartnerRedemptionService | POST /v1/{t}/catalogue/{id}/redeem | Handles unavailable partner API with pending state |
| Max per customer per reward | rewards_catalogue.max_per_customer_per_month | RewardsCatalogueService | — | COUNT check on redemption_log per customer |
| CREATE TABLE rewards_catalogue (
  id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id               VARCHAR(64) NOT NULL,
  reward_uid              VARCHAR(128) NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  reward_type             ENUM('VOUCHER','CASHBACK','PHYSICAL','DIGITAL','SERVICE','PARTNER') NOT NULL,
  points_cost             DECIMAL(18,4) NOT NULL,
  stock_quantity          INT,       -- NULL = unlimited
  low_stock_threshold     INT NOT NULL DEFAULT 10,
  max_per_customer_per_month INT NOT NULL DEFAULT 1,
  tier_access_min         VARCHAR(128),  -- NULL = available to all tiers
  status                  ENUM('ACTIVE','HIDDEN','OUT_OF_STOCK','DISCONTINUED') NOT NULL DEFAULT 'ACTIVE',
  valid_from              DATETIME(6),
  valid_until             DATETIME(6),
  is_partner_reward       TINYINT(1) NOT NULL DEFAULT 0,
  partner_id              VARCHAR(128),   -- FK to partner_reward_catalogue
  image_s3_path           VARCHAR(512),
  UNIQUE KEY uk_reward (tenant_id, reward_uid)
) ENGINE=InnoDB;

CREATE TABLE reward_redemption_log (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       VARCHAR(64) NOT NULL,
  customer_uid    VARCHAR(128) NOT NULL,
  reward_uid      VARCHAR(128) NOT NULL,
  ledger_entry_id BIGINT UNSIGNED NOT NULL,
  redeemed_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_customer_reward (tenant_id, customer_uid, reward_uid, redeemed_at)
) ENGINE=InnoDB; |
| --- |
| Stage | MySQL | Service | API | Roles |
| --- | --- | --- | --- | --- |
| 1. Registration | merchants INSERT status=REGISTRATION | MerchantOnboardingService | POST /v1/{t}/merchants | ADMIN |
| 2. Agreement | merchants.status=AGREEMENT + contract upload to S3 | MerchantOnboardingService | PUT /v1/{t}/merchants/{uid}/agreement | ADMIN + Finance approval |
| 3. Configuration (maker-checker) | approval_requests for merchant_config | ApprovalWorkflowService | PUT /v1/{t}/merchants/{uid}/config | ADMIN (requires approval) |
| 4. Integration / Sandbox test | merchants.api_credentials_vault_ref set | MerchantIntegrationService | POST /v1/{t}/merchants/{uid}/test-transaction | ADMIN + MERCHANT |
| 5. Go-Live | merchants.onboarding_stage=ACTIVE | MerchantOnboardingService | PUT /v1/{t}/merchants/{uid}/activate | ADMIN |
| Requirement | MySQL | Service | API | Notes |
| --- | --- | --- | --- | --- |
| Settlement statement generation | merchant_settlement_cycles + settlement_line_items | MerchantSettlementService (Spring Batch) | GET /v1/{t}/merchants/{uid}/settlements | Monthly batch; PDF/CSV to S3 |
| 5-day dispute window | merchant_settlement_cycles.status=UNDER_DISPUTE | SettlementDisputeService | POST /v1/{t}/merchants/{uid}/settlements/{id}/dispute | MERCHANT |
| Disputed amount on hold | status=UNDER_DISPUTE blocks payment | SettlementDisputeService | — | Finance team resolves via portal |
| Resolve in 10 business days | Ops SLA tracked via resolution_deadline DATE column | OpsPortalService | PUT /v1/{t}/settlements/{id}/resolve | OPERATIONS |
| CREATE TABLE settlement_line_items (
  id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id            VARCHAR(64) NOT NULL,
  settlement_cycle_id  BIGINT UNSIGNED NOT NULL,
  merchant_uid         VARCHAR(128) NOT NULL,
  transaction_ref      VARCHAR(128) NOT NULL,
  customer_uid         VARCHAR(128) NOT NULL,
  points_redeemed      DECIMAL(18,4) NOT NULL,
  monetary_value       DECIMAL(18,2) NOT NULL,
  transaction_date     DATE NOT NULL,
  is_disputed          TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB; |
| --- |
| POST /v1/{tenantId}/transactions/{eventId}/reverse
Auth: OPERATIONS role + maker-checker approval_request if > threshold
Body: { refundAmount: 1500.00, currency: 'INR', reason: 'MERCHANT_DISPUTE' }
Action:
  1. Lookup original points_ledger entry by source_event_id
  2. Compute points to reverse (full or partial)
  3. Check idempotency: idem_key = 'rev_' + original_idem_key — reject if exists
  4. BEGIN TRANSACTION
  5.   INSERT points_ledger REVERSAL entry
  6.   UPDATE customer_balance_snapshot
  7. COMMIT
  8. Publish ReversalCompleted Kafka event → notifications |
| --- |
| Requirement | MySQL Query | Service | API | Notes |
| --- | --- | --- | --- | --- |
| Real-time monetary liability | SELECT SUM(points) * tenant_config.points_currency_rate FROM points_ledger WHERE entry_type IN ('CREDIT',...) AND (expires_at IS NULL OR expires_at > NOW()) | LiabilityService | GET /v1/{t}/finance/liability | FINANCE, ADMIN |
| Liability by programme/tier/segment | GROUP BY source_campaign_id + JOIN customer_profiles.tier_id | LiabilityService | GET /v1/{t}/finance/liability?breakdownBy=tier | FINANCE |
| Monthly movement report | SUM credits, debits, expirations per period | LiabilityReportJob (Spring Batch) | GET /v1/{t}/finance/reports/liability-movement | FINANCE |
| Breakage forecast (3/6/12m) | Historical earn/burn velocity × projection model | LiabilityForecastService | GET /v1/{t}/finance/liability/forecast?months=6 | FINANCE |
| Provisioning rate configuration | tenant_config JSON field: provisioning_rate | TenantConfigService | PATCH /v1/{t}/config | ADMIN + Finance approval |
| Fraud Scenario | Detection Method | Redis / MySQL | Action on Detection |
| --- | --- | --- | --- |
| Self-referral | Check device_fingerprint + email_hash + phone_hash matches between referrer and referee | MySQL JOIN on customer_identity_links | INSERT fraud_alerts + referral.status=FRAUD_FLAGGED; points NOT awarded |
| Velocity abuse | Redis INCR earn_velocity:{t}:{cuid}:{hour} — if > threshold, flag | Redis counter + fraud_alerts INSERT | Points placed in fraud_blocked_events; not credited until cleared |
| Account takeover | New device fingerprint + unusual redemption pattern detected by Kafka Streams | Kafka Streams windowed aggregation | SMS security alert to customer; suspicious transactions to fraud review queue |
| Coupon stacking | At point of validation: check existing active coupon in Redis + is_stackable flag | Redis + MySQL coupons | 422 with 'Non-stackable coupon already applied' error |
| Merchant fraud | Transaction amount anomaly (> 3 std devs from merchant average) | ClickHouse merchant stats | Merchant suspended + settlement hold |
| Promotional exploitation | Cluster detection: many accounts from same IP/device | Kafka Streams cluster detection | Block IP in ip_blacklist + alert fraud team |
| Role | MySQL Access | Can Do | Cannot Do |
| --- | --- | --- | --- |
| SUPER_ADMIN | All tables (platform-level) | Manage all tenants, platform config, billing | Nothing |
| ADMIN (tenant-scoped) | All tables WHERE tenant_id=current | Full programme config, campaign setup, merchant onboarding | Approve own changes above threshold |
| PROGRAM_MANAGER | loyalty_rules, campaigns, tier_definitions | Create/edit rules and campaigns | Approve own changes; access finance reports |
| ANALYST | Read-only on all tables | View reports, dashboards, customer data (masked PII) | Any write operations |
| OPERATIONS | manual adjustments, fraud_alerts, disputes | Process adjustments, handle disputes, manage overrides | Change programme rules, create campaigns |
| FINANCE | finance/* endpoints | View/export all financial reports, set provisioning rate | Modify wallets or campaigns |
| SUPPORT | customer_profiles (read), points_ledger (read) | View customer loyalty history, raise adjustment for approval | Make unapproved changes |
| MERCHANT | campaigns (own merchant_id only) | Create/monitor own campaigns, view own settlement | See other merchants' data or customer PII |
| CUSTOMER | Self-service only (own profile + wallet) | View balance, redeem, update preferences | See other customers' data |
| Notification Event | Trigger | Channel | Transactional? | Opt-out Respected? |
| --- | --- | --- | --- | --- |
| Points earned | RewardIssued Kafka event | Push + SMS | Yes | No — always sent |
| Points redeemed | RedemptionCompleted Kafka event | Push + SMS + Email | Yes | No |
| Tier upgrade | TierUpgraded Kafka event | Push + Email | Yes | No |
| Tier downgrade warning (60 days) | TierDowngradeWarningJob | Push + Email + SMS | Yes | No |
| Points expiry 60-day warning | ExpiryNotificationJob | Email + Push | Yes | No |
| Points expiry 7-day warning | ExpiryNotificationJob | Push + SMS | Yes | No |
| New campaign offer | CampaignActivated Kafka event | Push + In-App + Email | No — marketing | Yes — check communication_prefs |
| Referral bonus awarded | ReferralMilestoneCompleted Kafka event | Push + Email | Yes | No |
| Fraud alert to customer | FraudAlertCreated Kafka event | SMS + Email | Yes — security | No |
| Monthly statement | Monthly batch | Email | No — marketing | Yes |
| Event | When Fired | Producer Service | Kafka Topic | Retry on Failure |
| --- | --- | --- | --- | --- |
| POINTS_EARNED | After ledger CREDIT INSERT committed | RewardEngine | platform.rewards.issued | Yes — webhook_delivery_log retry job |
| POINTS_REDEEMED | After ledger DEBIT INSERT committed | RedemptionService | platform.rewards.issued | Yes |
| TIER_UPGRADED | After customer_profiles.tier_id updated | TierEvaluationService | platform.tier.events | Yes |
| CAMPAIGN_BUDGET_EXHAUSTED | When campaign.status → EXHAUSTED | CampaignBudgetService | platform.campaign.alerts | Yes |
| REFERRAL_COMPLETED | When referral.status → REWARDED | ReferralMilestoneService | platform.referral.events | Yes |
| FRAUD_ALERT_RAISED | When fraud_alerts INSERT | FraudDetectionService | platform.fraud.alerts | Yes |
| Report | Data Source | Service | API Endpoint | Roles |
| --- | --- | --- | --- | --- |
| Customer enrolment by tier | MySQL customer_profiles GROUP BY tier_id | ReportingService | GET /v1/{t}/reports/membership | ADMIN, ANALYST |
| Points earned vs redeemed | MySQL points_ledger GROUP BY entry_type, DATE | ReportingService | GET /v1/{t}/reports/earn-burn | ADMIN, FINANCE |
| Campaign performance (reach, ROI) | MySQL campaign_participations + ClickHouse events | CampaignAnalyticsService | GET /v1/{t}/reports/campaigns | ADMIN, ANALYST |
| Referral effectiveness | MySQL referrals GROUP BY status | ReferralAnalyticsService | GET /v1/{t}/reports/referrals | ADMIN |
| Loyalty liability (real-time) | MySQL points_ledger SUM | LiabilityService | GET /v1/{t}/finance/liability | FINANCE, ADMIN |
| Merchant settlement | MySQL merchant_settlement_cycles | MerchantSettlementService | GET /v1/{t}/merchants/{uid}/settlements | FINANCE, MERCHANT |
| Breakage analysis | MySQL points_ledger WHERE entry_type='EXPIRE' | BreakageReportService | GET /v1/{t}/finance/reports/breakage | FINANCE |
| Churn prediction | ClickHouse (ML model output via MLflow) | MlInsightsService | GET /v1/{t}/analytics/churn-risk | ADMIN, ANALYST |
| Redis Key Pattern | Type | TTL | Owner Service | Purpose |
| --- | --- | --- | --- | --- |
| tenant:{tenantId}:rules:active | String (JSON) | 300s | RuleEngine | Cached active rules for tenant — invalidated by platform.config.updates Kafka event |
| tenant:{tenantId}:config | String (JSON) | 600s | All services | Cached tenant_config row — TTL refreshed on access |
| customer_balance:{tenantId}:{customerUid} | String (Decimal) | 3600s | RewardEngine / BalanceService | L1 balance cache — DECRBY/INCRBY on every ledger write. Deleted on redemption to force MySQL recompute if needed. |
| idem:{tenantId}:{idempotencyKey} | String (1) | 86400s | RewardEngine | Idempotency guard — SETNX before any ledger write. If key exists: duplicate, skip. |
| reserve:{tenantId}:{customerUid}:{requestUid} | String (points) | 86400s | PartnerRedemptionService | Soft-reserves points during pending partner API call. Released on COMPLETED or FAILED. |
| earn_cap:{tenantId}:{customerUid}:{YYYYMMDD} | String (counter) | 86400s | RewardEngine | Daily earn cap counter — INCR on every credit. Expires at end of day. |
| earn_cap_monthly:{tenantId}:{customerUid}:{YYYYMM} | String (counter) | 2678400s | RewardEngine | Monthly earn cap counter — expires at end of month. |
| freq:{tenantId}:{customerId}:{ruleId}:{YYYYMM} | String (counter) | 2678400s | RuleEngine | Frequency rule counter — e.g. '5th purchase this month'. INCR + compare to rule threshold. |
| streak:{tenantId}:{customerId}:{ruleId} | ZSet (score=epoch) | Configurable per rule | RuleEngine | Streak tracking — ZSet score is timestamp of each qualifying event. ZRANGEBYSCORE to count within window. |
| campaign_budget:{tenantId}:{campaignUid} | String (decimal) | Until campaign ends | CampaignService | NOT used for atomic decrement (MySQL handles that) — used as quick availability check cache only |
| leaderboard:{tenantId}:{type}:{scopeId} | ZSet (score=points) | 86400s | LeaderboardService | Real-time leaderboard rankings — ZADD on every point earn. ZREVRANK for customer rank lookup. |
| session:{sessionToken} | String (JSON) | 1800s | AuthService | Business Portal admin session — 30min TTL, refreshed on activity |
| coupon_used:{tenantId}:{couponCode}:{customerUid} | String (1) | Until coupon expires | CouponService | Prevents double-use of single-use coupons — SETNX pattern |
| # application.yml — Redis Cluster config
spring.data.redis.cluster.nodes:
  - redis-node-1:6379
  - redis-node-2:6380
  - redis-node-3:6381
spring.data.redis.password: ${REDIS_PASSWORD_FROM_VAULT}
spring.data.redis.ssl.enabled: true
spring.data.redis.lettuce.pool.max-active: 20
spring.data.redis.lettuce.pool.max-idle: 10
spring.data.redis.lettuce.pool.min-idle: 5

# Key namespace pattern: all keys MUST include tenantId as second segment
# This provides logical isolation within shared Redis cluster
# Pattern: {domain}:{tenantId}:{...rest} |
| --- |
| Topic Name | Partitions | Producers | Consumers | Payload Key Fields | Retention |
| --- | --- | --- | --- | --- | --- |
| {tenantId}.events.raw | 32 | WebhookReceiver, REST Ingestion API | KafkaStreams Enrichment Topology | tenantId, externalId, eventType, amount, transactionId | 7 days |
| {tenantId}.events.enriched | 32 | KafkaStreams Enrichment Topology | RuleEngine Service | tenantId, customerUid, eventType, customerProfile (merged) | 7 days |
| platform.rule.eval | 64 | KafkaStreams (from enriched) | RuleEngine Service pods | tenantId, customerUid, enrichedEvent | 7 days |
| platform.reward.commands | 64 | RuleEngine Service | RewardEngine Service | commandId, idempotencyKey, tenantId, customerUid, actionType, params | 30 days |
| platform.rewards.issued | 32 | RewardEngine Service | NotificationService, ClickHouseSink, CustomerAPI cache invalidation | tenantId, customerUid, points, ruleId, eventId | 30 days |
| platform.config.updates | 8 | RuleService (on rule activate/deactivate) | All RuleEngine pods | tenantId, ruleUid, changeType (ACTIVATED/DEACTIVATED) | 7 days |
| platform.tier.events | 16 | TierEvaluationService | NotificationService, CustomerAPI | tenantId, customerUid, previousTier, newTier, triggerEvent | 30 days |
| platform.campaign.alerts | 8 | CampaignBudgetService | NotificationService, AdminPortal | tenantId, campaignUid, alertType (BUDGET_80PCT/EXHAUSTED) | 7 days |
| platform.referral.events | 16 | ReferralMilestoneService | RewardEngine, NotificationService | tenantId, referralUid, referrerUid, refereeUid, stage | 30 days |
| platform.fraud.alerts | 8 | FraudDetectionService | FraudTeamQueue, NotificationService | tenantId, customerUid, fraudType, severity, triggeredRule | 90 days |
| platform.notifications | 16 | All services | NotificationService | tenantId, customerUid, notificationType, channel, templateId, params | 3 days |
| platform.audit.log | 16 | All services (any state mutation) | AuditLogSink (MySQL writer) | tenantId, actorId, action, entityType, entityId, beforeState, afterState | 365 days |
| platform.webhook.outbound | 16 | All event producers | WebhookDeliveryService | tenantId, eventType, payload, webhookSubscriptionIds | 7 days |
| platform.reward.commands.DLQ | 8 | RewardEngine (on max retries) | OpsAlertService | original message + failure reason + attempt count | 90 days |
| platform.partner.redemptions | 8 | PartnerRedemptionService | RetryPartnerRedemptionJob | tenantId, requestUid, partnerId, status, nextRetryAt | 30 days |
| Consumer Group ID | Consumes From | Scale Strategy | Exactly-Once? |
| --- | --- | --- | --- |
| rule-engine-cg | platform.rule.eval | KEDA: scale 2→100 pods based on consumer lag > 10,000 msgs | No (at-least-once; SpEL is idempotent) |
| reward-engine-cg | platform.reward.commands | KEDA: scale 2→50 pods based on lag > 5,000 msgs | YES — Kafka transactions + Redis idem key + MySQL UNIQUE constraint |
| notification-cg | platform.notifications | KEDA: scale 2→20 pods | At-least-once (duplicate notification acceptable) |
| audit-sink-cg | platform.audit.log | Fixed 4 pods (ordered within partition) | At-least-once (audit_log UNIQUE constraint prevents dups) |
| clickhouse-sink-cg | platform.rewards.issued, {t}.events.raw | Kafka Connect ClickHouse connector | At-least-once |
| webhook-delivery-cg | platform.webhook.outbound | KEDA: scale 2→20 pods | At-least-once (delivery log tracks state) |
| tier-eval-cg | platform.rewards.issued (filter CREDIT events) | Fixed 4 pods | At-least-once (tier check is idempotent) |
| Service Name | Owns (MySQL Tables) | Kafka: Produces To | Kafka: Consumes From | Key APIs |
| --- | --- | --- | --- | --- |
| ingestion-service | — | tenantId.events.raw | — | POST /v1/{t}/events (webhook + REST) |
| customer-profile-service | customer_profiles, customer_identity_links, customer_balance_snapshot | platform.notifications (enrolment events) | — | GET/PUT /v1/{t}/customers/*, POST /v1/{t}/enrol |
| rule-engine-service | loyalty_rules, loyalty_rules_history, rule_simulation_results | platform.reward.commands, platform.audit.log | platform.rule.eval, platform.config.updates | POST/GET/PUT/DELETE /v1/{t}/rules, /simulate, /rollback |
| reward-engine-service | points_ledger | platform.rewards.issued, platform.audit.log | platform.reward.commands | Internal only — no public API; driven by Kafka |
| redemption-service | reward_redemption_log, partner_redemption_requests | platform.rewards.issued | — | POST /v1/{t}/customers/{uid}/redeem, POST /v1/{t}/catalogue/{id}/redeem |
| tier-service | tier_definitions, tier_evaluation_log | platform.tier.events | platform.rewards.issued | GET/POST/PUT /v1/{t}/tiers |
| campaign-service | campaigns, campaign_participations, coupons | platform.campaign.alerts, platform.audit.log | — | POST/GET/PUT /v1/{t}/campaigns, /v1/{t}/coupons |
| referral-service | referral_programmes, referrals | platform.referral.events | platform.rewards.issued (watches txn events for milestones) | GET /v1/{t}/me/referral-code, /referrals/stats |
| merchant-service | merchants, merchant_settlement_cycles, settlement_line_items | platform.webhook.outbound | — | POST/GET/PUT /v1/{t}/merchants, /settlements |
| catalogue-service | rewards_catalogue, reward_redemption_log, partner_reward_catalogue | — | — | GET /v1/{t}/catalogue, POST /v1/{t}/catalogue/{id}/redeem |
| notification-service | notification_templates, notification_log | — | platform.notifications | GET /v1/{t}/notifications/templates (admin manage) |
| webhook-service | webhook_subscriptions, webhook_delivery_log | — | platform.webhook.outbound | GET/POST /v1/{t}/webhooks, /deliveries |
| fraud-service | fraud_alerts, fraud_blocked_events, device_blacklist | platform.fraud.alerts | All event topics (monitors patterns) | GET /v1/{t}/fraud/alerts |
| approval-service | approval_requests | platform.audit.log | — | POST /v1/{t}/approvals, PUT /v1/{t}/approvals/{uid}/review |
| coalition-service | coalition_partnerships, coalition_customer_links, coalition_settlement_records | platform.rewards.issued | — | POST/GET /v1/{t}/coalition/*, /transfer |
| reporting-service | Read-only views on all tables | — | — | GET /v1/{t}/reports/*, /finance/*, /analytics/* |
| gamification-service | missions, customer_mission_progress, badges, customer_badges, leaderboard_snapshots | platform.notifications | platform.rewards.issued (mission progress) | GET /v1/{t}/missions, /leaderboard, /badges |
| admin-portal-bff | No own tables — aggregates from above services | — | — | Business Portal backend-for-frontend aggregation layer |
| Metric | Target | MySQL Strategy | Alerting Threshold |
| --- | --- | --- | --- |
| Event ingestion throughput | 500K events/sec platform-wide | Webhook Receivers are stateless — MySQL not in hot path; all writes buffered via Kafka | Kafka consumer lag > 50,000 for >2 min |
| Rule evaluation P99 | < 20ms | Redis L1 cache for rules (300s TTL); MySQL only on cache miss (~0.1% of requests) | Rule eval latency > 50ms P99 |
| Ledger write P99 | < 500ms | HikariCP pool=50; InnoDB row-lock only for same idempotency_key; batch inserts not used | Ledger write > 1000ms P99 |
| Balance lookup P99 | < 100ms | Redis L1 (< 1ms); MySQL L2 snapshot (< 10ms); full SUM only for reconciliation | Balance lookup > 200ms P99 |
| Customer API (wallet) P99 | < 100ms | Redis wallet cache; MySQL indexed by (tenant_id, customer_uid) — PK or unique index always hit | Customer API > 200ms P99 |
| Coalition transfer P99 | < 1000ms | Single MySQL SERIALIZABLE transaction; 2 inserts; network latency dominant if cross-region | Coalition transfer > 2000ms P99 |
| Campaign budget decrement | < 5ms per call | Single atomic UPDATE (Gap Fix #5); InnoDB row-level lock — not table lock | Budget decrement > 50ms |
| Tier evaluation | < 5s from event ingestion | Async via Kafka; tier eval service is separate consumer group | Tier eval > 30s |
| -- Balance computation query (must hit index)
SELECT SUM(CASE WHEN entry_type IN ('CREDIT','COALITION_CREDIT','WELCOME_BONUS','REFERRAL_BONUS',
                                     'MANUAL_CREDIT','CAMPAIGN_BONUS') THEN points ELSE 0 END)
     - SUM(CASE WHEN entry_type IN ('DEBIT','EXPIRE','COALITION_DEBIT',
                                     'MANUAL_DEBIT','REVERSAL') THEN points ELSE 0 END)
FROM points_ledger
WHERE tenant_id = ? AND customer_uid = ?
  AND (expires_at IS NULL OR expires_at > NOW());
-- Uses: INDEX idx_balance (tenant_id, customer_uid, entry_type, expires_at)

-- Active rules for tenant (must hit covering index)
SELECT * FROM loyalty_rules
WHERE tenant_id = ? AND status = 'ACTIVE' AND trigger_event = ?
ORDER BY priority DESC;
-- Uses: INDEX idx_active_rules (tenant_id, status, priority, trigger_event)

-- Campaign budget decrement (atomic UPDATE — uses UK on campaign_uid + tenant_id)
UPDATE campaigns SET budget_consumed = budget_consumed + ?
WHERE tenant_id = ? AND campaign_uid = ? AND status = 'ACTIVE'
  AND (budget_consumed + ?) <= budget_total;
-- Uses: UNIQUE KEY uk_tenant_campaign (tenant_id, campaign_uid) |
| --- |
| Phase | Duration | What to Do | Rollback |
| --- | --- | --- | --- |
| 0 — Provision | Week 1-2 | Provision MySQL 8.0 Aurora cluster (Multi-AZ). Create all schemas via Flyway V1 migrations. Create DB users with least-privilege grants. Set up HikariCP pools. Configure Grafana MySQL dashboard. | Drop and re-provision — no data yet |
| 1 — Code Migration | Week 3-6 | Update all Spring Data JPA entities and repositories to MySQL dialect. Replace JSONB operators (::jsonb, @>) with JSON_EXTRACT(), JSON_CONTAINS(). Replace PostgreSQL arrays with JSON arrays. Update Flyway scripts from PG to MySQL syntax. Update all unit/integration tests to use MySQL (Testcontainers mysql:8.0). | Feature flag DATABASE_PROVIDER=postgres reverts to old stack |
| 2 — Data Migration | Week 6-7 | ETL: PostgreSQL → MySQL for all existing tenant/rule/config data. ETL: MongoDB → MySQL customer_profiles (JSON fields to MySQL JSON columns). Validate: row counts + SUM checksums + Finance sign-off on ledger balance match. | Dual-write window: writes go to both DBs during migration; switch read to MySQL after validation |
| 3 — Parallel Validation | Week 7-8 | Deploy MySQL-backed services to staging. Full E2E test suite. Load test at 2x peak (1M events/min). Cross-tenant isolation test. Coalition ACID test. Ledger reconciliation test. | Immediate revert to PostgreSQL+MongoDB if any test fails |
| 4 — Blue-Green Cutover | Week 8-9 | Deploy MySQL pods alongside existing. Shift traffic: 1% → 5% → 25% → 50% → 100% over 48h. Monitor Grafana: MySQL query P99, Kafka lag, error rate, HikariCP pool utilization. | Traffic shift back to old pods in <5 minutes (Kong weight adjustment) |
| 5 — Decommission | Week 10+ | After 2 weeks stable on MySQL: snapshot PostgreSQL + MongoDB to S3 (encrypted). Decommission instances. Update all documentation. | N/A — only proceed after 2-week stability window with Finance sign-off |
| Critical: Ledger Migration Sign-Off Required
The points ledger is a financial record. Before Phase 4 cutover, Finance team MUST sign off that: (1) all ledger row counts match between PostgreSQL and MySQL, (2) SUM(points) per customer per tenant matches exactly, (3) all idempotency_keys are preserved. No cutover without written Finance sign-off. |
| --- |
| Module | Test Scenario | Pass Condition | Tester |
| --- | --- | --- | --- |
| Enrolment | New customer signs up | Profile created within 3s; welcome bonus in wallet within 7s; push notification sent within 60s | QA + Ops |
| Points Accrual | Gold tier customer makes ₹2,000 purchase | Points = 20 * 2x tier multiplier = 40 within 7s; notification sent; ledger entry has correct source_event_id | Finance + QA |
| Points Redemption | Customer redeems 500 points for ₹50 cashback | Points deducted immediately; cashback credited; confirmation notification within 60s; negative balance impossible | Finance + QA |
| Points Expiry | Points with expires_at = tomorrow batch runs | Expired points marked EXPIRE in ledger; balance reduced; notification sent day-of; breakage report updated | Finance + QA |
| Tier Upgrade | Customer crosses 5,000pt Gold threshold | Tier updated within 5s; next transaction earns 2x; notification within 5 min; tier_evaluation_log entry created | QA + Ops |
| Tier Grace Period | Customer misses maintenance threshold at annual review | 60-day warning sent; tier NOT changed for 90 days; if still below: downgrade with notification | QA |
| Rule Simulation (Gap Fix #1) | Admin runs simulation on DRAFT rule against 90 days of history | Returns: events evaluated, customers affected, est. points, est. cost within 120s. No ledger writes. | QA + BA |
| Rule Rollback (Gap Fix #3) | Admin rolls back rule to version 2 | Rule reverts to v2 snapshot; all Rule Engine pods reload within 10s; history entry created | QA |
| Partner Redemption Pending (Gap Fix #2) | Partner API returns 503 during redemption | Points RESERVED (not deducted); status=PENDING_PARTNER; retry fires in 1min; customer notified of delay | QA + Ops |
| Inactivity Flag (Gap Fix #4) | Customer inactive 24+ months earns points | Flag cleared; re-engagement notification sent; points awarded; last_activity_at updated | QA |
| Campaign Budget Atomic | 1,000 concurrent redemptions against campaign with ₹500 budget | Campaign stops EXACTLY at ₹500 — no overspend under concurrent load; atomic UPDATE verified | QA + Load Test |
| Coalition Transfer | Customer transfers 200 Tenant A points to Tenant B | Tenant A ledger has COALITION_DEBIT; Tenant B ledger has COALITION_CREDIT; both balances correct; settlement record created | QA + Finance |
| Dual Ingestion ID_ONLY | Client sends external_id only (no customer block) | Stub profile created; points awarded correctly; event processed end-to-end | QA |
| Dual Ingestion FULL_PROFILE | Client sends full customer details | Full profile created with encrypted PII; subsequent ID_ONLY event matches to same profile | QA |
| Maker-Checker | One admin creates a rule; same admin tries to approve it | System rejects self-approval (CHECK constraint enforced); second admin can approve | QA + Security |
| GDPR Erasure | Customer requests data deletion | PII fields nulled in customer_profiles; ledger entries orphaned (no PII); deletion logged in audit_log; opted-out from all marketing | Compliance + QA |
| Tenant Isolation | Tenant A API call with Tenant B's customerUid | Returns 404 (not found in tenant A's data scope); no cross-tenant data leakage; alert fired | QA + Security |
| Fraud: Self-Referral | Customer creates second account with same device + phone | Referral flagged as FRAUD_FLAGGED within 10s; points NOT awarded; fraud team alerted | Security + QA |
| Merchant Settlement | Monthly batch runs for Merchant X | Settlement statement PDF generated and uploaded to S3; email sent to merchant; all line items traceable to source transactions | Finance + QA |
| Load Test | 500,000 events/min for 30 minutes | No Kafka lag > 50K; all rule evaluations < 20ms P99; all ledger writes < 500ms P99; zero duplicate ledger entries | DevOps + QA |
| BRD Coverage Confirmation
All 23 functional modules from BRD Section 4 are covered by this architecture. All 5 architecture gaps are resolved. All UAT acceptance criteria are defined. This document is sufficient for Cursor or any development team to begin building without ambiguity. |
| --- |
| Portal | Users | Key Pages | Auth |
| --- | --- | --- | --- |
| Business Portal (Admin) | ADMIN, PROGRAM_MANAGER, ANALYST, FINANCE, OPERATIONS, SUPPORT | Dashboard, Rule Builder, Campaign Manager, Customer Lookup, Reports, Tier Config, Merchant Onboarding | NextAuth.js 5 + Keycloak OIDC + RBAC middleware |
| Merchant Portal | MERCHANT role | My Campaigns, Settlement Reports, Campaign Create, Budget Monitor | Separate Keycloak realm — merchant-specific |
| Customer-Facing Web (optional) | CUSTOMER token | My Wallet, Transaction History, Rewards Catalogue, Referral Page, Tier Progress | JWT issued by client's own auth — platform validates signature against tenant's registered public key |
| Term | Definition |
| --- | --- |
| Append-Only Ledger | points_ledger table where rows are INSERT-only. MySQL TRIGGER prevents UPDATE/DELETE. Balance always derived by SUM computation. |
| Atomic Decrement | Single MySQL UPDATE statement that decrements a counter and checks a constraint atomically — no separate SELECT required. Prevents race conditions under concurrent load. |
| Coalition | Formal partnership between two tenants enabling cross-platform earn/redeem. Single MySQL ACID transaction ensures no points lost in transfer. |
| Customer UID | Internal platform UUID for a customer. Never exposed externally (use external_id for client-facing references). |
| DRY_RUN mode | Rule simulation execution context where SpEL evaluates conditions and formulas but no ledger writes or Kafka events are produced. |
| Dual Ingestion | Two supported API modes: ID_ONLY (external_id only) and FULL_PROFILE (full PII details). Both result in the same downstream processing. |
| External ID | Client's own identifier for a customer, sent in event payload. Platform maps to internal customer_uid. |
| Idempotency Key | SHA-256(tenantId + eventId + ruleId) stored as UNIQUE KEY on points_ledger. Prevents duplicate reward issuance from Kafka redelivery. |
| Maker-Checker | Four-eyes approval: the user who creates/requests a high-impact action cannot also approve it. Enforced by approval_requests.CHECK constraint. |
| RLS Equivalent | PostgreSQL Row-Level Security implemented in MySQL via 5-layer defense: JPA interceptor, AOP assertion, MySQL DB user privileges, CI integration tests, and Kong tenant header. |
| Stub Profile | customer_profiles row in ID_ONLY mode: external_id set, PII fields null. Can be upgraded to FULL_PROFILE when client sends customer block. |
| Tenant | A business entity on the platform. All tables have tenant_id as second column. All queries MUST include it for isolation. |