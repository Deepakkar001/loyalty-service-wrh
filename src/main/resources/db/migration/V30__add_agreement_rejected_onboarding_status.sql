-- Add AGREEMENT_REJECTED to tenant onboarding status enum
-- Required for the new explicit state when admin rejects an agreement.

ALTER TABLE tenant_onboarding
    MODIFY COLUMN onboarding_status ENUM(
        'PENDING_EMAIL_VERIFICATION','EMAIL_VERIFIED',
        'AGREEMENT_PENDING','AGREEMENT_SIGNED','AGREEMENT_REJECTED',
        'CONFIGURED','RULES_CONFIGURED','SANDBOX_TESTING',
        'ACTIVE','SUSPENDED','TERMINATED'
    ) NOT NULL DEFAULT 'PENDING_EMAIL_VERIFICATION';

