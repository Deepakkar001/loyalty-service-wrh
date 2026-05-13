package com.loyaltyos.onboarding.statemachine;

import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import org.junit.jupiter.api.Test;

import static com.loyaltyos.onboarding.domain.enums.OnboardingStatus.*;
import static org.assertj.core.api.Assertions.*;

class OnboardingStateMachineTest {

    @Test
    void validTransitions_shouldReturnTrue() {
        assertThat(PENDING_EMAIL_VERIFICATION.canTransitionTo(EMAIL_VERIFIED)).isTrue();
        assertThat(EMAIL_VERIFIED.canTransitionTo(AGREEMENT_PENDING)).isTrue();
        assertThat(AGREEMENT_PENDING.canTransitionTo(AGREEMENT_SIGNED)).isTrue();
        assertThat(AGREEMENT_SIGNED.canTransitionTo(CONFIGURED)).isTrue();
        assertThat(CONFIGURED.canTransitionTo(RULES_CONFIGURED)).isTrue();
        assertThat(RULES_CONFIGURED.canTransitionTo(SANDBOX_TESTING)).isTrue();
        assertThat(SANDBOX_TESTING.canTransitionTo(ACTIVE)).isTrue();
        assertThat(ACTIVE.canTransitionTo(SUSPENDED)).isTrue();
        assertThat(ACTIVE.canTransitionTo(TERMINATED)).isTrue();
        assertThat(SUSPENDED.canTransitionTo(ACTIVE)).isTrue();
    }

    @Test
    void skippingStages_shouldReturnFalse() {
        assertThat(PENDING_EMAIL_VERIFICATION.canTransitionTo(ACTIVE)).isFalse();
        assertThat(CONFIGURED.canTransitionTo(ACTIVE)).isFalse();
        assertThat(EMAIL_VERIFIED.canTransitionTo(CONFIGURED)).isFalse();
    }

    @Test
    void terminatedIsTerminal_shouldHaveNoValidTransitions() {
        for (OnboardingStatus status : OnboardingStatus.values()) {
            assertThat(TERMINATED.canTransitionTo(status)).isFalse();
        }
    }

    @Test
    void backwardsTransition_shouldReturnFalse() {
        assertThat(ACTIVE.canTransitionTo(CONFIGURED)).isFalse();
        assertThat(SANDBOX_TESTING.canTransitionTo(CONFIGURED)).isFalse();
    }
}

