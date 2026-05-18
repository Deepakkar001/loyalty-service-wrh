package com.loyaltyos.onboarding.analytics.service;

import com.loyaltyos.onboarding.analytics.dto.CohortRetentionRow;
import com.loyaltyos.onboarding.analytics.dto.PointsActivityRow;
import com.loyaltyos.onboarding.analytics.dto.RuleEffectivenessRow;
import com.loyaltyos.onboarding.analytics.dto.RulePerformanceRow;
import com.loyaltyos.onboarding.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.onboarding.analytics.dto.TierDistributionRow;
import com.loyaltyos.onboarding.analytics.dto.TierUpgradeCohortRow;
import com.loyaltyos.onboarding.analytics.dto.TierVelocityBucketRow;
import com.loyaltyos.onboarding.analytics.repository.AnalyticsQueryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public AnalyticsService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    public List<PointsActivityRow> getPointsActivity(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        return analyticsQueryRepository.getPointsActivity(tenantId, programmeUid, from, to);
    }

    public List<RulePerformanceRow> getRulePerformance(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        return analyticsQueryRepository.getRulePerformance(tenantId, programmeUid, from, to);
    }

    public List<TierDistributionRow> getTierDistribution(String tenantId, String programmeUid) {
        return analyticsQueryRepository.getTierDistribution(tenantId, programmeUid);
    }

    public List<SegmentAnalysisRow> getEngagementSegments(String tenantId, String programmeUid) {
        return analyticsQueryRepository.getEngagementSegments(tenantId, programmeUid);
    }

    public List<SegmentAnalysisRow> getBalanceBrackets(String tenantId, String programmeUid) {
        return analyticsQueryRepository.getBalanceBrackets(tenantId, programmeUid);
    }

    public List<CohortRetentionRow> getRetentionCohort(String tenantId, String programmeUid) {
        return analyticsQueryRepository.getRetentionCohort(tenantId, programmeUid);
    }

    public List<TierUpgradeCohortRow> getTierUpgradeCohort(String tenantId, String programmeUid) {
        return analyticsQueryRepository.getTierUpgradeCohort(tenantId, programmeUid);
    }

    public List<TierVelocityBucketRow> getTierVelocityBuckets(
        String tenantId,
        String programmeUid,
        String tierName
    ) {
        return analyticsQueryRepository.getTierVelocityBuckets(tenantId, programmeUid, tierName);
    }

    public List<RuleEffectivenessRow> getRuleEffectiveness(
        String tenantId,
        String programmeUid,
        String ruleUid,
        LocalDate from,
        LocalDate to
    ) {
        return analyticsQueryRepository.getRuleEffectiveness(tenantId, programmeUid, ruleUid, from, to);
    }
}
