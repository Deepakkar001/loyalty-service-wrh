package com.loyaltyos.analytics.dto;

import java.util.List;

public record DashboardEngagementSummary(
    double activePct,
    List<SegmentAnalysisRow> segments
) {}
