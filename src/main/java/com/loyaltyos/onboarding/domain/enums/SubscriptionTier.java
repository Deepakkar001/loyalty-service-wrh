package com.loyaltyos.onboarding.domain.enums;

/**
 * Tenant subscription tier — controls feature limits.
 * STANDARD     : max 50 active rules, basic features
 * PROFESSIONAL : max 500 active rules, advanced analytics, merchant portal
 * ENTERPRISE   : unlimited rules, coalition, AI, dedicated support
 *
 * TODO [BUSINESS]: Confirm exact per-tier limits for events/month, API rate limits,
 *                  and which feature_flags are unlocked per tier.
 */
public enum SubscriptionTier {
    STANDARD,
    PROFESSIONAL,
    ENTERPRISE
}

