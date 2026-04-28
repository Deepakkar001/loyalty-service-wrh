package com.loyaltyos.onboarding.exception;

import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(OnboardingStatus from, OnboardingStatus to) {
        super("Cannot transition onboarding status from " + from + " to " + to);
    }
}

