package com.loyaltyos.merchants.service;

import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.service.CampaignAnalyticsService;
import com.loyaltyos.merchants.dto.MerchantOpsSummaryResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantOpsSummaryService {

    private final MerchantRepository merchantRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignAnalyticsService campaignAnalyticsService;

    public MerchantOpsSummaryService(
        MerchantRepository merchantRepository,
        CampaignRepository campaignRepository,
        CampaignAnalyticsService campaignAnalyticsService
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.campaignAnalyticsService = Objects.requireNonNull(
            campaignAnalyticsService, "campaignAnalyticsService");
    }

    @Transactional(readOnly = true)
    public MerchantOpsSummaryResponse getOpsSummary(String tenantId, String merchantUid) {
        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));

        List<Campaign> campaigns = campaignRepository.findByTenantIdAndMerchantIdOrderByCreatedAtDesc(
            tenantId, merchantUid);

        MerchantOpsSummaryResponse response = new MerchantOpsSummaryResponse();
        response.setTotalCampaigns(campaigns.size());
        response.setIntegrationTestPassed(merchant.getIntegrationTestPassedAt() != null);

        int active = 0;
        int pending = 0;
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal consumed = BigDecimal.ZERO;
        long participations = 0;

        for (Campaign campaign : campaigns) {
            if (campaign.isPendingMerchantApproval()) {
                pending++;
            }
            if (campaign.getStatus() == CampaignStatus.ACTIVE) {
                active++;
            }
            if (campaign.getBudgetTotal() != null) {
                allocated = allocated.add(campaign.getBudgetTotal());
            }
            if (campaign.getBudgetConsumed() != null) {
                consumed = consumed.add(campaign.getBudgetConsumed());
            }
            try {
                CampaignStatsResponse stats = campaignAnalyticsService.getCampaignStats(
                    tenantId, campaign.getCampaignUid());
                participations += stats.getTotalParticipations();
            } catch (RuntimeException ignored) {
                // skip campaigns without stats
            }
        }

        response.setActiveCampaigns(active);
        response.setPendingApprovalCampaigns(pending);
        response.setTotalBudgetAllocated(allocated);
        response.setTotalBudgetConsumed(consumed);
        response.setTotalParticipations(participations);
        return response;
    }
}
