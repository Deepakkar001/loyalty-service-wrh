package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record DashboardTopRuleRow(
    String ruleUid,
    String ruleName,
    long evaluationCount,
    BigDecimal totalPointsAwarded
) {}
