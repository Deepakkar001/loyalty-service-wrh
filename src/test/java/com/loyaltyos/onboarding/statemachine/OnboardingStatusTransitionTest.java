package com.loyaltyos.onboarding.statemachine;

import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import org.junit.jupiter.api.Test;

import static com.loyaltyos.onboarding.domain.enums.OnboardingStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OnboardingStatusTransitionTest {

    @Test
    void validForwardTransitions() {
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
        assertThat(SUSPENDED.canTransitionTo(TERMINATED)).isTrue();
    }

    @Test
    void skippingStagesMustFail() {
        assertThat(PENDING_EMAIL_VERIFICATION.canTransitionTo(ACTIVE)).isFalse();
        assertThat(PENDING_EMAIL_VERIFICATION.canTransitionTo(CONFIGURED)).isFalse();
        assertThat(CONFIGURED.canTransitionTo(ACTIVE)).isFalse();
        assertThat(EMAIL_VERIFIED.canTransitionTo(CONFIGURED)).isFalse();
        assertThat(AGREEMENT_SIGNED.canTransitionTo(ACTIVE)).isFalse();
    }

    @Test
    void terminatedIsTerminalWithNoExits() {
        for (OnboardingStatus s : OnboardingStatus.values()) {
            assertThat(TERMINATED.canTransitionTo(s))
                .as("TERMINATED should not be able to transition to " + s)
                .isFalse();
        }
    }

    @Test
    void backwardsTransitionsMustFail() {
        assertThat(ACTIVE.canTransitionTo(CONFIGURED)).isFalse();
        assertThat(SANDBOX_TESTING.canTransitionTo(CONFIGURED)).isFalse();
        assertThat(AGREEMENT_SIGNED.canTransitionTo(EMAIL_VERIFIED)).isFalse();
        assertThat(CONFIGURED.canTransitionTo(AGREEMENT_PENDING)).isFalse();
    }
}

