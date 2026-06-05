package com.loyaltyos.support.support;

import com.loyaltyos.onboarding.enums.SubscriptionTier;

public final class SupportSlaSupport {

    private SupportSlaSupport() {}

    public static String responseHint(SubscriptionTier tier, boolean urgent) {
        if (tier == null) {
            tier = SubscriptionTier.STANDARD;
        }
        if (urgent) {
            return switch (tier) {
                case ENTERPRISE -> "Urgent: target first response within 1 hour (24/7 for production outages).";
                case PROFESSIONAL -> "Urgent: target first response within 4 hours on business days.";
                default -> "Urgent: target first response within 8 business hours.";
            };
        }
        return switch (tier) {
            case ENTERPRISE -> "Target first response within 4 business hours.";
            case PROFESSIONAL -> "Target first response within 1 business day.";
            default -> "Target first response within 2 business days.";
        };
    }
}
