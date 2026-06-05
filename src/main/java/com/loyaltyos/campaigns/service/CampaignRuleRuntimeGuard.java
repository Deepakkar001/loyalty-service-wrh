package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared runtime eligibility for CAMPAIGN earn rules (production events only).
 */
@Service
public class CampaignRuleRuntimeGuard {

    private final CampaignRepository campaignRepository;
    private final CampaignEligibilityService eligibilityService;
    private final CampaignParticipationRepository participationRepository;

    public CampaignRuleRuntimeGuard(
        CampaignRepository campaignRepository,
        CampaignEligibilityService eligibilityService,
        CampaignParticipationRepository participationRepository
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.eligibilityService = Objects.requireNonNull(eligibilityService, "eligibilityService");
        this.participationRepository = Objects.requireNonNull(participationRepository, "participationRepository");
    }

    @Transactional(readOnly = true)
    public boolean isCampaignRuleEligible(
        String tenantId,
        String campaignUid,
        String customerId,
        CampaignEventContext event,
        Instant now
    ) {
        if (campaignUid == null || campaignUid.isBlank()) {
            return false;
        }
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid.trim())
            .orElse(null);
        if (campaign == null) {
            return false;
        }
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            return false;
        }
        if (campaign.getValidFrom() != null && now.isBefore(campaign.getValidFrom())) {
            return false;
        }
        if (campaign.getValidUntil() != null && now.isAfter(campaign.getValidUntil())) {
            return false;
        }
        if (event != null && !eligibilityService.matchesTriggerEventType(campaign, event.eventType())) {
            return false;
        }
        if (!eligibilityService.matchesCustomerScope(tenantId, campaign, customerId)) {
            return false;
        }
        if (event != null && !eligibilityService.matchesTargetSegment(campaign, event)) {
            return false;
        }
        if (exceedsMaxPerCustomer(tenantId, campaign, customerId)) {
            return false;
        }
        if (exceedsMaxParticipations(tenantId, campaign)) {
            return false;
        }
        return true;
    }

    private boolean exceedsMaxPerCustomer(String tenantId, Campaign campaign, String customerId) {
        Integer max = campaign.getMaxPerCustomer();
        if (max == null || max <= 0) {
            return false;
        }
        long count = participationRepository.countByTenantIdAndCampaignUidAndCustomerId(
            tenantId,
            campaign.getCampaignUid(),
            customerId
        );
        return count >= max;
    }

    private boolean exceedsMaxParticipations(String tenantId, Campaign campaign) {
        Integer max = campaign.getMaxParticipations();
        if (max == null || max <= 0) {
            return false;
        }
        long count = participationRepository.countByTenantIdAndCampaignUid(tenantId, campaign.getCampaignUid());
        return count >= max;
    }
}
