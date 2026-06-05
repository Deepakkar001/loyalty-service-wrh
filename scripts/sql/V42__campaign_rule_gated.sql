-- Rule-gated campaign flow: execution mode + server-side sandbox pass record

ALTER TABLE campaigns
    ADD COLUMN execution_mode VARCHAR(32) NOT NULL DEFAULT 'RULE_GATED';

-- Existing ACTIVE campaigns without a linked CAMPAIGN earn rule keep offer_config path during migration.
UPDATE campaigns c
SET c.execution_mode = 'LEGACY_OFFER'
WHERE c.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM earn_rules r
      WHERE r.tenant_id = c.tenant_id
        AND r.campaign_uid = c.campaign_uid
        AND r.rule_type = 'CAMPAIGN'
  );

CREATE TABLE IF NOT EXISTS campaign_rule_sandbox_pass (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    campaign_uid VARCHAR(128) NOT NULL,
    rule_uid VARCHAR(128) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    targeted_check_ok TINYINT(1) NOT NULL DEFAULT 1,
    passed_by VARCHAR(255) NULL,
    passed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_tenant_rule_sandbox_pass UNIQUE (tenant_id, rule_uid),
    INDEX idx_sandbox_pass_campaign (tenant_id, campaign_uid)
);
