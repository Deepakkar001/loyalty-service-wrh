package com.loyaltyos.onboarding.rewards.exception;

/**
 * Some idempotency keys for this issuance already exist on the ledger, but not all — unsafe to continue.
 */
public class RewardPartialIdempotencyException extends RuntimeException {

    public RewardPartialIdempotencyException(String message) {
        super(message);
    }
}
