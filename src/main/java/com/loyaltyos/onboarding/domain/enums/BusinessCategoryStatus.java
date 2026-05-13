package com.loyaltyos.onboarding.domain.enums;

/**
 * Moderation lifecycle for {@code ref_business_category} rows.
 *
 * <p>Seeded categories ship as {@link #APPROVED}. Tenant-typed "Other" submissions land as
 * {@link #PENDING_REVIEW} and only become visible in the public onboarding dropdown after an
 * admin moves them to {@link #APPROVED} (or hides them via {@link #REJECTED}).</p>
 */
public enum BusinessCategoryStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
