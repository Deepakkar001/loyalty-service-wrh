package com.loyaltyos.onboarding.domain.enums;

/**
 * Operational status of a fully onboarded tenant.
 * Distinct from OnboardingStatus — this applies after a tenant reaches ACTIVE.
 */
public enum TenantStatus {
    ACTIVE,
    SUSPENDED,
    OFFBOARDED
}

