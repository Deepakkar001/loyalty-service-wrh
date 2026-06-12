package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record LiabilityProgrammeRollupRow(
    String programmeUid,
    long memberCount,
    BigDecimal outstandingPoints,
    BigDecimal outstandingMonetary,
    BigDecimal periodPointsIssued,
    BigDecimal periodPointsRedeemed,
    BigDecimal periodPointsExpired,
    BigDecimal periodNetChangePoints,
    BigDecimal periodNetChangeMonetary
) {}
