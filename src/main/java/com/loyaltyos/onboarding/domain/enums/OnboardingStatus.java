package com.loyaltyos.onboarding.domain.enums;

import java.util.Map;
import java.util.Set;

public enum OnboardingStatus {

    PENDING_EMAIL_VERIFICATION,
    EMAIL_VERIFIED,
    AGREEMENT_PENDING,
    AGREEMENT_SIGNED,
    CONFIGURED,
    SANDBOX_TESTING,
    ACTIVE,
    SUSPENDED,
    TERMINATED;

    private static final Map<OnboardingStatus, Set<OnboardingStatus>> VALID_TRANSITIONS =
        Map.of(
            PENDING_EMAIL_VERIFICATION, Set.of(EMAIL_VERIFIED),
            EMAIL_VERIFIED,             Set.of(AGREEMENT_PENDING),
            AGREEMENT_PENDING,          Set.of(AGREEMENT_SIGNED),
            AGREEMENT_SIGNED,           Set.of(CONFIGURED, AGREEMENT_PENDING),
            CONFIGURED,                 Set.of(SANDBOX_TESTING),
            SANDBOX_TESTING,            Set.of(ACTIVE),
            ACTIVE,                     Set.of(SUSPENDED, TERMINATED),
            SUSPENDED,                  Set.of(ACTIVE, TERMINATED),
            TERMINATED,                 Set.of()
        );

    public boolean canTransitionTo(OnboardingStatus next) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}

