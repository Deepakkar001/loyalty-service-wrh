package com.loyaltyos.analytics.dto;

import java.time.Instant;
import java.util.List;

public record DashboardOverviewResponse(
    String programmeUid,
    boolean hasData,
    DashboardKpiMetric activeMembers,
    DashboardKpiMetric pointsIssuedToday,
    DashboardKpiMetric redemptionsToday,
    DashboardKpiMetric avgOrderValue,
    DashboardKpiMetric atRiskMemberPct,
    List<DashboardVolumePoint> volumeSeries,
    List<TierDistributionRow> tierDistribution,
    List<DashboardTopRuleRow> topRules,
    List<DashboardRedemptionRow> topRedemptions,
    DashboardEngagementSummary engagement,
    DashboardRetentionSummary retention,
    DashboardPointsEconomics pointsEconomics,
    Instant generatedAt
) {}
