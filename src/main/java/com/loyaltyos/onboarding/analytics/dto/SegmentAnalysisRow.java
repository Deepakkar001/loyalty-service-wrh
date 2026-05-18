package com.loyaltyos.onboarding.analytics.dto;

import java.math.BigDecimal;

public record SegmentAnalysisRow(
    String segment,
    long memberCount,
    BigDecimal avgBalance,
    BigDecimal totalPointsHeld
) {}
