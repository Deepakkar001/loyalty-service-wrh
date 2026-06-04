package com.loyaltyos.referrals.model;

public enum ReferralRuleTrigger {
    /** Evaluated when referee links a referral code. */
    LINK,
    /** Evaluated when a PURCHASE integration event is processed for the referee. */
    PURCHASE,
    /**
     * Evaluated when a non-purchase integration event matches {@link ReferralRuleCriteria#getEventTypes()}.
     * Examples: PROFILE_COMPLETED, CUSTOMER_ENROLLED.
     */
    INTEGRATION_EVENT
}
