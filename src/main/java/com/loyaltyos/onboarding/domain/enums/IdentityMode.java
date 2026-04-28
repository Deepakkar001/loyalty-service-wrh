package com.loyaltyos.onboarding.domain.enums;

/**
 * How the tenant identifies their end-users.
 *
 * ID_ONLY      — tenant sends only their own customer identifier (external_id).
 *                No PII collected. Email/SMS notifications unavailable.
 *                Set at registration. CANNOT be changed after first event ingestion.
 *
 * FULL_PROFILE — tenant sends full customer PII (name, email, phone, DOB).
 *                PII is AES-256 encrypted at application layer before INSERT.
 *                All notification channels available.
 *
 * BOTH         — tenant supports both modes (same programme).
 *                Profile is created as ID_ONLY stub; upgraded to FULL_PROFILE
 *                when the tenant later sends customer block in event payload.
 */
public enum IdentityMode {
    ID_ONLY,
    FULL_PROFILE,
    BOTH
}

