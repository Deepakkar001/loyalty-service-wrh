-- Manual schema change (Flyway not used). Phase 2B: voucher reward tracking + fraud review audit action.

ALTER TABLE referral_rewards_issued
  ADD COLUMN reward_type VARCHAR(16) NOT NULL DEFAULT 'POINTS' AFTER recipient_type,
  ADD COLUMN catalog_reward_uid VARCHAR(64) NULL AFTER reward_type;

-- Extend audit action enum (MySQL: modify column; ignore error if APPROVED already exists)
ALTER TABLE referral_audit_log
  MODIFY COLUMN action ENUM(
    'CODE_CREATED','CODE_REVOKED','LINKED','STAGE_COMPLETED','REWARD_ISSUED',
    'FRAUD_FLAGGED','REJECTED','OVERRIDE','APPROVED'
  ) NOT NULL;
