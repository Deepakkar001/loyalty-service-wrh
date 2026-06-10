package com.loyaltyos.analytics.model;

/**
 * Tier identity and display labels for API responses around a points movement.
 */
public record CustomerTierOutcome(
    String tierBeforeUid,
    String tierAfterUid,
    String tierBeforeName,
    String tierAfterName,
    boolean tierChanged
) {}
