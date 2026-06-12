package com.loyaltyos.merchants.enums;

import java.util.Map;
import java.util.Set;

public enum MerchantOnboardingStage {

    REGISTRATION,
    AGREEMENT,
    CONFIGURATION,
    INTEGRATION,
    ACTIVE,
    SUSPENDED;

    private static final Map<MerchantOnboardingStage, Set<MerchantOnboardingStage>> VALID_TRANSITIONS =
        Map.ofEntries(
            Map.entry(REGISTRATION, Set.of(AGREEMENT, SUSPENDED)),
            Map.entry(AGREEMENT, Set.of(CONFIGURATION, REGISTRATION, SUSPENDED)),
            Map.entry(CONFIGURATION, Set.of(INTEGRATION, AGREEMENT, SUSPENDED)),
            Map.entry(INTEGRATION, Set.of(ACTIVE, CONFIGURATION, SUSPENDED)),
            Map.entry(ACTIVE, Set.of(SUSPENDED)),
            Map.entry(SUSPENDED, Set.of(ACTIVE))
        );

    public boolean canTransitionTo(MerchantOnboardingStage next) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
