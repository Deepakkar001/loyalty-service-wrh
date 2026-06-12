package com.loyaltyos.merchants.exception;

import com.loyaltyos.merchants.enums.MerchantOnboardingStage;

public class InvalidMerchantStateTransitionException extends RuntimeException {
    public InvalidMerchantStateTransitionException(MerchantOnboardingStage from, MerchantOnboardingStage to) {
        super("Cannot transition merchant onboarding from " + from + " to " + to);
    }
}
