package com.loyaltyos.onboarding.domain.enums;

import java.util.Map;
import java.util.Set;

public enum OnboardingStatus {

    PENDING_EMAIL_VERIFICATION,
    EMAIL_VERIFIED,
    AGREEMENT_PENDING,
    AGREEMENT_SIGNED,
    AGREEMENT_REJECTED,
    CONFIGURED,
    RULES_CONFIGURED,
    SANDBOX_TESTING,
    ACTIVE,
    SUSPENDED,
    TERMINATED;

    private static final Map<OnboardingStatus, Set<OnboardingStatus>> VALID_TRANSITIONS =
        Map.ofEntries(
            Map.entry(PENDING_EMAIL_VERIFICATION, Set.of(EMAIL_VERIFIED)),
            Map.entry(EMAIL_VERIFIED,             Set.of(AGREEMENT_PENDING)),
            Map.entry(AGREEMENT_PENDING,          Set.of(AGREEMENT_SIGNED)),
            Map.entry(AGREEMENT_SIGNED,           Set.of(CONFIGURED, AGREEMENT_PENDING, AGREEMENT_REJECTED)),
            Map.entry(AGREEMENT_REJECTED,         Set.of(AGREEMENT_PENDING)),
            Map.entry(CONFIGURED,                 Set.of(RULES_CONFIGURED)),
            Map.entry(RULES_CONFIGURED,           Set.of(SANDBOX_TESTING)),
            Map.entry(SANDBOX_TESTING,            Set.of(ACTIVE)),
            Map.entry(ACTIVE,                     Set.of(SUSPENDED, TERMINATED)),
            Map.entry(SUSPENDED,                  Set.of(ACTIVE, TERMINATED)),
            Map.entry(TERMINATED,                 Set.of())
        );

    public boolean canTransitionTo(OnboardingStatus next) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}

