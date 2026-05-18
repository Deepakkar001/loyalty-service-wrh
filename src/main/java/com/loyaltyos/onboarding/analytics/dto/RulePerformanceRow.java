package com.loyaltyos.onboarding.analytics.dto;

import java.math.BigDecimal;

public record RulePerformanceRow(
    String ruleUid,
    String ruleName,
    String status,
    long evaluationCount,
    long successCount,
    BigDecimal totalPointsAwarded
) {}
