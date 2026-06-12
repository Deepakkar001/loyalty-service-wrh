package com.loyaltyos.merchants.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MerchantOnboardingStageTest {

    @Test
    void registrationToAgreement() {
        assertTrue(MerchantOnboardingStage.REGISTRATION.canTransitionTo(MerchantOnboardingStage.AGREEMENT));
    }

    @Test
    void registrationToActiveInvalid() {
        assertFalse(MerchantOnboardingStage.REGISTRATION.canTransitionTo(MerchantOnboardingStage.ACTIVE));
    }

    @Test
    void suspendFromAnyNonSuspendedStage() {
        for (MerchantOnboardingStage stage : MerchantOnboardingStage.values()) {
            if (stage != MerchantOnboardingStage.SUSPENDED) {
                assertTrue(
                    stage.canTransitionTo(MerchantOnboardingStage.SUSPENDED),
                    "Expected suspend from " + stage
                );
            }
        }
    }
}
