# LoyaltyOS Rule Engine — Production-Grade Implementation Prompt (v2 — Repo-Aligned)

**Purpose:** Single implementation spec for the **Rule Engine** inside the **existing** Spring Boot onboarding service (`backend/`). This document **supersedes** ad-hoc copies of “v1” prompts that assumed PostgreSQL + Kafka.

**Author stance:** Senior Java/Spring architect — modular layers, testable seams, **no Kafka** in Phase 1, **MySQL 8** only, **Redis** for cache + counters.

**Build order:** Flyway → entities/repos → SpEL + parser (hardened) → `RuleEvaluationEngine` → optional Redis cache → REST → tests → wire sandbox/ingestion.

---

## PHASE 0: PRE-IMPLEMENTATION CHECKLIST (CORRECTED FOR THIS REPO)

### Existing infrastructure (validate before coding)

- [ ] **MySQL 8** running (local or Docker) — **not** PostgreSQL; this project uses `spring.jpa` + MySQL dialect.
- [ ] **Redis 7.x** from `docker-compose` (`backend/docker-compose.yml`) — used for rule list cache + optional frequency/cap counters.
- [ ] **Kafka:** intentionally **disabled** in this repo (see `KafkaConfig.java`, `build.gradle`). **Do not** add Kafka consumers/producers for rule-engine v1. Use **in-process** calls + optional `@Async` later.
- [ ] **Spring Boot 3.3 + Java 21** — already set in `build.gradle`.
- [ ] **Flyway:** migrations **V1–V22** applied (`programmes`, `programme_config`, `tier_definitions` scoped by `programme_uid`, `tenant_config`, webhooks, API keys, etc.).
- [ ] **Next Flyway files:** start at **`V23__...`** (nothing reuses V4–V6 from older drafts — those numbers are already consumed in this repo).

### Architecture alignment

- [ ] **Monolith:** Rule engine = **package** under `com.loyaltyos.onboarding...` (recommended: `com.loyaltyos.onboarding.rules` or `...engine.rule`) — **not** a separate deployable.
- [ ] **No Kafka** for cache invalidation: on rule CRUD / programme save, **delete Redis keys** or bump a **`rulesVersion`** in Redis/DB and include it in cache keys.
- [ ] **BRD §4.8:** support **priority**, **FIRST_MATCH / ALL_MATCHING**, **Best-for-Customer / Best-for-Business** using **programme conflict policy** already in canonical config (`tenant_config` / programme JSON). Persist an **evaluation audit** row (JSON trace) for Finance.

### Data model (confirm / create)

| Artifact | Status in repo | Action |
|----------|----------------|--------|
| `tenant_onboarding` / `tenant_config` | Exists | Load earn rate, caps, conflict policy, `programme_uid` default `"default"` |
| `tier_definitions` | Exists | Load multiplier by **customer’s current tier** (from profile or tier rank) |
| `programme_config` (canonical JSON) | Exists | Source of truth for base economics; rules **layer on top** |
| **`earn_rules`**, **`rule_conditions`**, **`rule_actions`** | **New** | Flyway V23–V25 |
| **`points_ledger`** (append-only) | **New** — *owned by Reward Engine* | Flyway **V26** (or separate doc); rule engine **must not** write ledger in v1 if you want clean boundaries — **recommended:** rule engine **only** returns `RewardCommand` DTOs; a later `RewardExecutionService` writes ledger. |
| **`customer_*` loyalty profile** | **May not exist yet** | MVP: `customerId` as **external id string** + optional stub table `customer_loyalty_profile` OR read from future `customer_profiles` — **do not** assume a `customers` table exists until you add a migration. |

---

## ARCHITECTURE OVERVIEW (4 LAYERS)

```
┌─────────────────────────────────────────────────────────┐
│  LAYER 4: REST — POST /api/v1/engine/rule/evaluate      │
│           (+ JWT: tenant must match body or use JWT-only) │
├─────────────────────────────────────────────────────────┤
│  LAYER 3: RuleEvaluationService                         │
│           (load rules → SpEL match → formulas → caps     │
│            → conflict policy → RewardCommand list)       │
├─────────────────────────────────────────────────────────┤
│  LAYER 2: ConditionTreeParser, SpelEvaluationService,     │
│           ProgrammeConfigLoader, TierMultiplierResolver │
├─────────────────────────────────────────────────────────┤
│  LAYER 1: MySQL (earn_rules, rule_conditions, rule_actions) │
│           Redis (optional cache + freq/cap keys)         │
└─────────────────────────────────────────────────────────┘
```

**Synchronous only** for v1. Ingestion or sandbox calls this service as a **plain Spring bean**.

---

## PHASE 1: DATABASE SCHEMA (Flyway V23–V26)

> **MySQL syntax.** Use `DATETIME(6)` and `JSON` as already used elsewhere in this project.

### V23__create_earn_rules_and_rule_change_log.sql

```sql
-- Core rule definitions (tenant-scoped; programme-scoped for multi-program tenants)
CREATE TABLE earn_rules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    rule_uid VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority INT NOT NULL DEFAULT 0,
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    trigger_event_type VARCHAR(64) NOT NULL,
    execution_mode ENUM('FIRST_MATCH', 'ALL_MATCHING') NOT NULL DEFAULT 'ALL_MATCHING',
    effective_at DATETIME(6) NULL,
    end_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    activated_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    UNIQUE KEY uk_tenant_programme_rule (tenant_id, programme_uid, rule_uid),
    INDEX idx_tenant_prog_status_trigger (tenant_id, programme_uid, status, trigger_event_type),
    INDEX idx_tenant_prog_priority (tenant_id, programme_uid, priority)
) ENGINE=InnoDB;

CREATE TABLE rule_change_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    change_type ENUM('CREATED', 'UPDATED', 'STATUS_CHANGED', 'DELETED') NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    before_state JSON NULL,
    after_state JSON NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_rule_change_rule FOREIGN KEY (rule_id) REFERENCES earn_rules(id) ON DELETE CASCADE,
    INDEX idx_tenant_rule (tenant_id, rule_id)
) ENGINE=InnoDB;

-- ROLLBACK (manual):
-- DROP TABLE IF EXISTS rule_change_log;
-- DROP TABLE IF EXISTS earn_rules;
```

### V24__create_rule_conditions.sql

```sql
CREATE TABLE rule_conditions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    condition_tree JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_rule_conditions_rule (rule_id),
    CONSTRAINT fk_rule_conditions_rule FOREIGN KEY (rule_id) REFERENCES earn_rules(id) ON DELETE CASCADE,
    INDEX idx_tenant_rule (tenant_id, rule_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS rule_conditions;
```

### V25__create_rule_actions.sql

```sql
CREATE TABLE rule_actions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    action_uid VARCHAR(128) NOT NULL,
    action_type ENUM(
        'AWARD_POINTS',
        'GRANT_BADGE',
        'ISSUE_VOUCHER',
        'TRIGGER_NOTIFICATION',
        'WEBHOOK_CALLBACK',
        'UPGRADE_TIER',
        'APPLY_MULTIPLIER'
    ) NOT NULL,
    formula VARCHAR(512) NULL,
    config JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_action (tenant_id, action_uid),
    CONSTRAINT fk_rule_actions_rule FOREIGN KEY (rule_id) REFERENCES earn_rules(id) ON DELETE CASCADE,
    INDEX idx_tenant_rule (tenant_id, rule_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS rule_actions;
```

### V26__create_points_ledger_and_balance_cache.sql

> **Boundary:** Prefer implementing **ledger writes** in a **`RewardExecutionService`** in the same monolith. Rule evaluation returns **commands only**. If you co-locate ledger in the same PR, document that `RuleEvaluationEngine` **never** inserts into `points_ledger`.

```sql
CREATE TABLE points_ledger (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    idempotency_key VARCHAR(128) NOT NULL,
    entry_type ENUM('CREDIT','DEBIT','EXPIRE','REVERSAL','ADJUST') NOT NULL,
    points DECIMAL(18, 4) NOT NULL,
    source_rule_id BIGINT UNSIGNED NULL,
    source_event_id VARCHAR(128) NULL,
    source_campaign_id VARCHAR(128) NULL,
    expires_at DATETIME(6) NULL,
    description VARCHAR(512) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_customer_idempotency (tenant_id, customer_id, idempotency_key),
    INDEX idx_tenant_customer (tenant_id, customer_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB;

CREATE TABLE customer_balance_cache (
    tenant_id VARCHAR(64) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    customer_id VARCHAR(128) NOT NULL,
    balance DECIMAL(18, 4) NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (tenant_id, programme_uid, customer_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS customer_balance_cache;
-- DROP TABLE IF EXISTS points_ledger;
```

---

## PHASE 2–3: JAVA PACKAGES, ENTITIES, REPOSITORIES

**Package recommendation:** `com.loyaltyos.onboarding.rules` (keeps component scanning simple under existing `@SpringBootApplication`).

### Entity / JPA fixes vs naive draft

1. **`rule_uid`:** `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "programme_uid", "rule_uid"}))` — **not** globally unique.
2. **Enums:** top-level `RuleStatus`, `ExecutionMode`, `ActionType`, `LedgerEntryType` — **not** nested inside entity files (JPA + style).
3. **`EarnRule` ↔ `RuleCondition`:** use `@OneToOne` with **owning side** on `RuleCondition` (`@JoinColumn(name="rule_id")`) or map `conditions` as lazy with fetch join in repository query — avoid broken `mappedBy` on both sides.
4. **`PointsLedger`:** if ledger lives in same module, put entity under `...rules.ledger` or `...rewards` package to signal ownership.

### Repository JPQL (Hibernate 6)

- Avoid `CURRENT_TIMESTAMP` in JPQL against `Instant` fields unless you pass **`Instant now = Instant.now()`** as `:now` parameter — portable and testable.
- Example filter: `(r.effectiveAt IS NULL OR r.effectiveAt <= :now) AND (r.endAt IS NULL OR r.endAt > :now)`.

---

## PHASE 4: SpEL & CONDITION TREE (SECURITY + CORRECTNESS)

### Must-have hardening

1. **No** `T(java...)` / class literals — use a **custom `EvaluationContext`** or `SimpleEvaluationContext.forReadOnlyDataBinding()` with **explicit root objects** only: `#event`, `#customer`, `#tenant`, `#now`.
2. **Property allow-list:** derive allowed keys from **tenant event schema** + fixed core fields (`amount`, `eventType`, `channel`, …).
3. **`ConditionTreeParser`:** inject `SpelExpressionParser` via constructor — **do not** leave `expressionParser` field uninitialized (draft bug).
4. **Leaf vs branch nodes:** document JSON shape: either `{ "op":"AND", "nodes":[...] }` **or** leaf `{ "field":"event.amount","op":"GTE","value":500 }` — parser must distinguish **logical** vs **comparison** nodes without ambiguous `"operator"` overload.

### `SpelEvaluationService`

- `boolean match(String expression, EvaluationContext ctx)`
- `Number evaluateFormula(String formula, EvaluationContext ctx)` with **null-safe** handling and capped decimal scale (`RoundingMode.HALF_UP`).

---

## PHASE 5: RULE EVALUATION SERVICE (CORE)

### Responsibilities

1. Resolve **`programmeUid`** (default `"default"` or from request once API supports it).
2. Load **ACTIVE** rules for `(tenantId, programmeUid, triggerEventType)` ordered by **`priority DESC`**.
3. Build **one** `EvaluationContext` per request (reuse for all rules) — **fix draft bug** where maps were built but never passed to `SpELEvaluator.createContext(...)`.
4. For each rule: parse condition JSON → SpEL → if true, evaluate **each** `AWARD_POINTS` action formula → accumulate points.
5. **`FIRST_MATCH`:** stop after **first rule that yields any points** (or first rule that matches — product decision; document choice).
6. **`ALL_MATCHING`:** sum across rules; then apply **tenant daily/monthly caps** from programme config + Redis counters.
7. **Tier multiplier:** resolve from **`tier_definitions`** for customer’s tier (not hardcoded `switch` on string in draft).
8. **Conflict policy:** when multiple rules produce competing outcomes, apply **BRD §4.8.4** using stored **BEST_FOR_CUSTOMER / BEST_FOR_BUSINESS** from programme config.
9. **Output:** `RuleEvaluationResponse` with `matchedRules[]`, `suppressedRules[]` (reason), `finalPoints`, `rewardCommands[]`, `evaluationTrace` (JSON for audit table).

### Dependencies to **inject** (draft referenced missing types)

- `EarnRuleRepository`
- `ProgrammeConfigService` or dedicated **loader** that reads canonical JSON + `tier_definitions`
- Optional: `CustomerLoyaltyProfileRepository` (stub until real customers table exists)
- Optional: `RuleCacheService`
- **Not** `PointsLedgerRepository` inside rule evaluation if ledger is reward-layer only.

---

## PHASE 6: REDIS CACHING (REVISED)

**Do not** use `RedisTemplate<String, EarnRule>` (JPA entity serialization is fragile).

**Recommended:**

- Cache key: `rules:v1:{tenantId}:{programmeUid}:{eventType}` → value = **`JSON` list of rule IDs + version hash** or cached **DTO** built from DB.
- On **rule create/update/status change:** `redisTemplate.delete(keysMatching(...))` or increment `rulesVersion` and embed in key.

---

## PHASE 7: REST CONTROLLER

- Path: **`POST /api/v1/engine/rule/evaluate`** (as in original spec).
- **Security:** reuse existing JWT filter — **tenant JWT** must imply `tenantId`; either **strip `tenantId` from body** and take from principal only, or **validate body matches JWT** to prevent cross-tenant calls.
- Return **422** with field errors for validation; **400** for malformed condition JSON.

---

## PHASE 8: TESTS

1. **Unit:** `ConditionTreeParserTest` — AND/OR/NOT, leaf operators, invalid tree errors.
2. **Unit:** `SpelEvaluationServiceTest` — whitelist blocks `T(` attempts.
3. **Integration:** `@DataJpaTest` or `@SpringBootTest` with Testcontainers MySQL (optional) — one **seeded** ACTIVE rule + `evaluate()` returns expected points.
4. **Contract:** REST test with `@AutoConfigureMockMvc` + JWT test helper (match existing security tests pattern if any).

---

## PHASE 9: INTEGRATION (NO KAFKA)

- **`IntegrationService`** sandbox path: build `RuleEvaluationRequest` from payload → call `RuleEvaluationEngine.evaluate()` → persist result to **`sandbox_test_events`** (add migration if table missing) or reuse existing validate-event flow.
- **Future `EventIngestionService`:** same call → then `rewardExecutionService.execute(commands)` in **same transaction** or **outbox table** later — still no Kafka.

---

## PHASE 10: CONFIGURATION (`application.yml`)

```yaml
loyalty:
  rules:
    cache-ttl-seconds: ${RULES_CACHE_TTL_SECONDS:300}
    max-condition-depth: ${RULES_MAX_CONDITION_DEPTH:32}
    evaluation-timeout-ms: ${RULES_EVAL_TIMEOUT_MS:5000}
```

(Reuse existing `spring.data.redis` — do not duplicate `spring.redis` tree unless you standardize on one.)

**Beans:** `SpelExpressionParser` as singleton; Redis template for **String** or **JSON** values only.

---

## PHASE 11: ACCEPTANCE CRITERIA (REALISTIC)

### Functional

- [ ] ACTIVE rules filtered by `trigger_event_type`, `effective_at`/`end_at`, `programme_uid`.
- [ ] Tier multiplier matches **`tier_definitions`** for the customer’s tier.
- [ ] **Caps** enforced when Redis enabled; graceful skip when Redis down (log + fail-open or fail-closed — **document**).
- [ ] **Idempotency** at **reward** layer via `idempotency_key` — rule engine returns stable command idempotency key = hash(`tenantId|programmeUid|customerId|eventId|ruleId`) or single event-level key per product choice.
- [ ] **Audit:** every evaluation persists trace (async acceptable).

### Performance (targets, not blockers)

- [ ] P99 **< 20ms** for **0 DB rules** + cache hit (lab); measure with Micrometer timer.
- [ ] Document degradation when cache cold + N rules.

### Security

- [ ] SpEL sandbox blocks class resolution / `T(`.
- [ ] Tenant isolation on **every** query: `WHERE tenant_id = ?` — enforce in repository **or** shared Hibernate filter if you add one later.

---

## BUILD & VERIFY

```bash
cd backend
./gradlew.bat flywayInfo
./gradlew.bat test
./gradlew.bat bootRun --args="--spring.profiles.active=local"
```

**Smoke curl:** `POST /api/v1/engine/rule/evaluate` with a valid JWT (reuse auth from tenant login).

---

## SUMMARY: WHAT CHANGED VS GENERIC “v1” PROMPT

| Topic | Old / generic prompt | **This repo (v2)** |
|--------|----------------------|---------------------|
| Database | PostgreSQL | **MySQL 8** |
| Flyway numbers | V4–V6 for rules | **V23+** only |
| Kafka | Sometimes assumed | **Explicitly off** |
| `rule_uid` UNIQUE | Global | **`(tenant_id, programme_uid, rule_uid)`** |
| Ledger | Inside rule engine | **Reward layer** recommended; same Flyway OK if documented |
| Redis value | `EarnRule` entity | **JSON / DTO / rule version key** |
| JPQL time | `CURRENT_TIMESTAMP` | **`:now` parameter** |
| Customers table | Assumed | **Stub or future migration** |
| BRD §4.8 | Implicit | **Conflict policy + audit trace** explicit |

---

## END OF PROMPT (v2)

When implementing in Cursor, attach: this file, `programme-config.schema.json`, `TenantConfigService` / `ProgrammeService`, and existing `SecurityConfig` for JWT paths.
