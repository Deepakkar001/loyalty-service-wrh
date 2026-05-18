package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.DropReason;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.campaigns.model.DroppedCampaign;
import com.loyaltyos.campaigns.model.EligibilityResult;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignEligibilityService {

    private final CampaignRepository campaignRepository;
    private final CampaignParticipationRepository participationRepository;
    private final CampaignJsonSupport jsonSupport;

    public CampaignEligibilityService(
        CampaignRepository campaignRepository,
        CampaignParticipationRepository participationRepository,
        CampaignJsonSupport jsonSupport
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.participationRepository = Objects.requireNonNull(participationRepository, "participationRepository");
        this.jsonSupport = Objects.requireNonNull(jsonSupport, "jsonSupport");
    }

    @Transactional(readOnly = true)
    public EligibilityResult findQualifying(String tenantId, String programmeUid, CampaignEventContext event) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(programmeUid, "programmeUid");
        Objects.requireNonNull(event, "event");

        Instant now = Instant.now();
        List<Campaign> active = campaignRepository.findActiveForEligibility(
            tenantId,
            programmeUid,
            event.eventType().trim(),
            now
        );

        List<Campaign> qualifying = new ArrayList<>();
        List<DroppedCampaign> dropped = new ArrayList<>();

        for (Campaign campaign : active) {
            if (!matchesTargetSegment(campaign, event)) {
                dropped.add(dropped(campaign, DropReason.ELIGIBILITY_FAILED));
                continue;
            }
            if (exceedsMaxPerCustomer(tenantId, campaign, event.customerId())) {
                dropped.add(dropped(campaign, DropReason.CUSTOMER_CAP_REACHED));
                continue;
            }
            if (exceedsMaxParticipations(tenantId, campaign)) {
                dropped.add(dropped(campaign, DropReason.TOTAL_PARTICIPATION_CAP_REACHED));
                continue;
            }
            qualifying.add(campaign);
        }

        return new EligibilityResult(qualifying, dropped);
    }

    private boolean matchesTargetSegment(Campaign campaign, CampaignEventContext event) {
        CampaignTargetSegment segment = jsonSupport.parseTargetSegment(campaign.getTargetSegment());

        if (segment.tierUids() != null && !segment.tierUids().isEmpty()) {
            String tier = event.customerTierUid();
            if (tier == null || tier.isBlank()) {
                return false;
            }
            boolean match = segment.tierUids().stream()
                .filter(Objects::nonNull)
                .map(t -> t.trim())
                .anyMatch(t -> t.equals(tier.trim()));
            if (!match) {
                return false;
            }
        }

        if (segment.channels() != null && !segment.channels().isEmpty()) {
            String channel = event.channel();
            if (channel == null || channel.isBlank()) {
                return false;
            }
            boolean match = segment.channels().stream()
                .filter(Objects::nonNull)
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .anyMatch(c -> c.equals(channel.trim().toUpperCase(Locale.ROOT)));
            if (!match) {
                return false;
            }
        }

        if (segment.minAmount() != null) {
            BigDecimal amount = event.amount() == null ? BigDecimal.ZERO : event.amount();
            if (amount.compareTo(segment.minAmount()) < 0) {
                return false;
            }
        }

        if (segment.countries() != null && !segment.countries().isEmpty()) {
            String country = event.country();
            if (country == null || country.isBlank()) {
                return false;
            }
            boolean match = segment.countries().stream()
                .filter(Objects::nonNull)
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .anyMatch(c -> c.equals(country.trim().toUpperCase(Locale.ROOT)));
            if (!match) {
                return false;
            }
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

    private static DroppedCampaign dropped(Campaign campaign, DropReason reason) {
        return new DroppedCampaign(campaign.getCampaignUid(), campaign.getName(), reason);
    }
}
