package com.loyaltyos.merchants.service;

import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.service.CampaignAnalyticsService;
import com.loyaltyos.merchants.dto.MerchantCampaignSummaryRow;
import com.loyaltyos.merchants.dto.MerchantDashboardStatsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantDashboardService {

    private final CampaignRepository campaignRepository;
    private final CampaignAnalyticsService analyticsService;

    public MerchantDashboardService(
        CampaignRepository campaignRepository,
        CampaignAnalyticsService analyticsService
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    @Transactional(readOnly = true)
    public MerchantDashboardStatsResponse getDashboardStats(String tenantId, String merchantUid) {
        List<Campaign> campaigns = campaignRepository
            .findByTenantIdAndMerchantIdOrderByCreatedAtDesc(tenantId, merchantUid);

        MerchantDashboardStatsResponse stats = new MerchantDashboardStatsResponse();
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalConsumed = BigDecimal.ZERO;
        long totalParticipations = 0;
        BigDecimal totalPointsIssued = BigDecimal.ZERO;
        int active = 0;
        int pending = 0;

        for (Campaign campaign : campaigns) {
            BigDecimal budgetTotal = nullToZero(campaign.getBudgetTotal());
            BigDecimal budgetConsumed = nullToZero(campaign.getBudgetConsumed());
            totalAllocated = totalAllocated.add(budgetTotal);
            totalConsumed = totalConsumed.add(budgetConsumed);
            if (campaign.isPendingMerchantApproval()) {
                pending++;
            }
            if (campaign.getStatus() == CampaignStatus.ACTIVE) {
                active++;
            }
            CampaignStatsResponse campaignStats = analyticsService.getCampaignStats(
                tenantId, campaign.getCampaignUid());
            totalParticipations += campaignStats.getTotalParticipations();
            if (campaignStats.getTotalPointsIssued() != null) {
                totalPointsIssued = totalPointsIssued.add(campaignStats.getTotalPointsIssued());
            }
        }

        stats.setActiveCampaigns(active);
        stats.setPendingApprovalCampaigns(pending);
        stats.setTotalBudgetAllocated(totalAllocated);
        stats.setTotalBudgetConsumed(totalConsumed);
        stats.setBudgetConsumedPct(consumedPct(totalConsumed, totalAllocated));
        stats.setTotalParticipations(totalParticipations);
        stats.setTotalPointsIssued(totalPointsIssued);

        // recentCampaigns reuses already-fetched stats for the first 5 campaigns
        List<MerchantCampaignSummaryRow> recent = campaigns.stream()
            .limit(5)
            .map(c -> toSummaryRow(tenantId, c))
            .toList();
        stats.setRecentCampaigns(recent);
        return stats;
    }

    private MerchantCampaignSummaryRow toSummaryRow(String tenantId, Campaign campaign) {
        CampaignStatsResponse campaignStats = analyticsService.getCampaignStats(
            tenantId,
            campaign.getCampaignUid()
        );
        MerchantCampaignSummaryRow row = new MerchantCampaignSummaryRow();
        row.setCampaignUid(campaign.getCampaignUid());
        row.setName(campaign.getName());
        row.setStatus(campaign.getStatus() != null ? campaign.getStatus().name() : null);
        row.setBudgetTotal(nullToZero(campaign.getBudgetTotal()));
        row.setBudgetConsumed(nullToZero(campaign.getBudgetConsumed()));
        row.setBudgetConsumedPct(campaignStats.getBudgetConsumedPct());
        row.setParticipations(campaignStats.getTotalParticipations());
        row.setPendingMerchantApproval(campaign.isPendingMerchantApproval());
        return row;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal consumedPct(BigDecimal consumed, BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return consumed.multiply(new BigDecimal("100"))
            .divide(total, 2, RoundingMode.HALF_UP);
    }
}
