package com.loyaltyos.onboarding.domain.enums;

/**
 * Data residency region for the tenant's data.
 * Determines which MySQL Aurora cluster stores their data.
 * Set at registration — CANNOT be changed.
 * TODO [BUSINESS + LEGAL]: Confirm required regions and any data localisation laws per region.
 */
public enum DataResidencyRegion {
    IN,   // India
    US,   // United States
    EU,   // European Union (GDPR)
    APAC  // Asia-Pacific (excl. India)
}

