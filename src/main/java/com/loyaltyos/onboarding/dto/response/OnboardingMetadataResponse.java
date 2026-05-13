package com.loyaltyos.onboarding.dto.response;

import java.util.List;

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

    public static class Option {
        private String value;
        private String label;

        public Option() {}

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String value;
            private String label;

            private Builder() {}

            public Builder value(String value) { this.value = value; return this; }
            public Builder label(String label) { this.label = label; return this; }

            public Option build() { return new Option(value, label); }
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
    }

    public OnboardingMetadataResponse() {}

    public OnboardingMetadataResponse(
        List<Option> businessCategories,
        List<Option> countries,
        List<Option> timezones,
        List<Option> businessModels,
        List<Option> annualRevenueRanges,
        List<Option> paymentMethodsAccepted,
        List<Option> settlementFrequencies,
        List<Option> currencies,
        List<Option> billingPaymentMethods,
        List<Option> contractDurations
    ) {
        this.businessCategories = businessCategories;
        this.countries = countries;
        this.timezones = timezones;
        this.businessModels = businessModels;
        this.annualRevenueRanges = annualRevenueRanges;
        this.paymentMethodsAccepted = paymentMethodsAccepted;
        this.settlementFrequencies = settlementFrequencies;
        this.currencies = currencies;
        this.billingPaymentMethods = billingPaymentMethods;
        this.contractDurations = contractDurations;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
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

        private Builder() {}

        public Builder businessCategories(List<Option> businessCategories) { this.businessCategories = businessCategories; return this; }
        public Builder countries(List<Option> countries) { this.countries = countries; return this; }
        public Builder timezones(List<Option> timezones) { this.timezones = timezones; return this; }
        public Builder businessModels(List<Option> businessModels) { this.businessModels = businessModels; return this; }
        public Builder annualRevenueRanges(List<Option> annualRevenueRanges) { this.annualRevenueRanges = annualRevenueRanges; return this; }
        public Builder paymentMethodsAccepted(List<Option> paymentMethodsAccepted) { this.paymentMethodsAccepted = paymentMethodsAccepted; return this; }
        public Builder settlementFrequencies(List<Option> settlementFrequencies) { this.settlementFrequencies = settlementFrequencies; return this; }
        public Builder currencies(List<Option> currencies) { this.currencies = currencies; return this; }
        public Builder billingPaymentMethods(List<Option> billingPaymentMethods) { this.billingPaymentMethods = billingPaymentMethods; return this; }
        public Builder contractDurations(List<Option> contractDurations) { this.contractDurations = contractDurations; return this; }

        public OnboardingMetadataResponse build() {
            return new OnboardingMetadataResponse(
                businessCategories,
                countries,
                timezones,
                businessModels,
                annualRevenueRanges,
                paymentMethodsAccepted,
                settlementFrequencies,
                currencies,
                billingPaymentMethods,
                contractDurations
            );
        }
    }

    public List<Option> getBusinessCategories() { return businessCategories; }
    public List<Option> getCountries() { return countries; }
    public List<Option> getTimezones() { return timezones; }
    public List<Option> getBusinessModels() { return businessModels; }
    public List<Option> getAnnualRevenueRanges() { return annualRevenueRanges; }
    public List<Option> getPaymentMethodsAccepted() { return paymentMethodsAccepted; }
    public List<Option> getSettlementFrequencies() { return settlementFrequencies; }
    public List<Option> getCurrencies() { return currencies; }
    public List<Option> getBillingPaymentMethods() { return billingPaymentMethods; }
    public List<Option> getContractDurations() { return contractDurations; }
}
