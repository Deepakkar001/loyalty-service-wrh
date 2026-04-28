package com.loyaltyos.onboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OnboardingMetadataResponse {
    private List<Option> businessCategories;
    private List<Option> countries;
    private List<Option> timezones;
    private List<Option> businessModels;
    private List<Option> annualRevenueRanges;
    private List<Option> paymentMethodsAccepted;
    private List<Option> settlementFrequencies;
    private List<Option> currencies;
    private List<Option> billingPaymentMethods;
    private List<Option> contractDurations;

    @Getter
    @Builder
    public static class Option {
        private String value;
        private String label;
    }
}
