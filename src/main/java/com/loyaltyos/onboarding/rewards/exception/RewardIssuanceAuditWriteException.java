package com.loyaltyos.onboarding.rewards.exception;

/**
 * Raised when {@code loyalty.rewards.issuance-audit-strict=true} and a SUCCESS audit row cannot be persisted.
 * Propagates to roll back issuance so ledger and audit stay consistent.
 */
public class RewardIssuanceAuditWriteException extends RuntimeException {

    public RewardIssuanceAuditWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
