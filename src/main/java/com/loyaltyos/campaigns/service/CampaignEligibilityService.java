package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.enums.DropReason;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.campaigns.model.DroppedCampaign;
import com.loyaltyos.campaigns.model.EligibilityResult;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetCustomerRepository;
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
    private final CampaignTargetCustomerRepository targetCustomerRepository;
    private final CampaignJsonSupport jsonSupport;

    public CampaignEligibilityService(
        CampaignRepository campaignRepository,
        CampaignParticipationRepository participationRepository,
        CampaignTargetCustomerRepository targetCustomerRepository,
        CampaignJsonSupport jsonSupport
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.participationRepository = Objects.requireNonNull(participationRepository, "participationRepository");
        this.targetCustomerRepository = Objects.requireNonNull(targetCustomerRepository, "targetCustomerRepository");
        this.jsonSupport = Objects.requireNonNull(jsonSupport, "jsonSupport");
    }

    @Transactional(readOnly = true)
    public EligibilityResult findQualifying(String tenantId, String programmeUid, CampaignEventContext event) {
        return findQualifying(tenantId, programmeUid, event, null);
    }

    /**
     * @param scopedCampaignUid when non-blank, only that campaign is loaded and evaluated
     */
    @Transactional(readOnly = true)
    public EligibilityResult findQualifying(
        String tenantId,
        String programmeUid,
        CampaignEventContext event,
        String scopedCampaignUid
    ) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(programmeUid, "programmeUid");
        Objects.requireNonNull(event, "event");

        if (scopedCampaignUid != null && !scopedCampaignUid.isBlank()) {
            return findQualifyingForCampaign(tenantId, programmeUid, event, scopedCampaignUid.trim());
        }

        Instant now = Instant.now();
        List<Campaign> active = campaignRepository.findActiveForEligibility(
            tenantId,
            programmeUid,
            event.eventType().trim(),
            now
        );
        return evaluateCandidates(tenantId, event, active);
    }

    private EligibilityResult findQualifyingForCampaign(
        String tenantId,
        String programmeUid,
        CampaignEventContext event,
        String campaignUid
    ) {
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));

        if (!programmeUid.equals(campaign.getProgrammeUid())) {
            throw new CampaignBadRequestException(
                "campaignUid does not belong to programmeUid " + programmeUid
            );
        }

        Instant now = Instant.now();
        assertCampaignRunnable(campaign, event.eventType().trim(), now);
        return evaluateCandidates(tenantId, event, List.of(campaign));
    }

    private void assertCampaignRunnable(Campaign campaign, String eventType, Instant now) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new CampaignBadRequestException(
                "Campaign is not active: " + campaign.getCampaignUid() + " (status=" + campaign.getStatus() + ")"
            );
        }
        if (campaign.getValidFrom() != null && now.isBefore(campaign.getValidFrom())) {
            throw new CampaignBadRequestException("Campaign is not yet valid: " + campaign.getCampaignUid());
        }
        if (campaign.getValidUntil() != null && now.isAfter(campaign.getValidUntil())) {
            throw new CampaignBadRequestException("Campaign has expired: " + campaign.getCampaignUid());
        }
        if (!matchesTriggerEventType(campaign, eventType)) {
            throw new CampaignBadRequestException(
                "Campaign does not accept eventType " + eventType + ": " + campaign.getCampaignUid()
            );
        }
    }

    public static boolean matchesTriggerEventType(Campaign campaign, String eventType) {
        String trigger = campaign.getTriggerEventType();
        if (trigger == null || trigger.isBlank() || eventType == null || eventType.isBlank()) {
            return false;
        }
        String normalized = eventType.trim();
        String raw = trigger.trim();
        if (raw.equals(normalized)) {
            return true;
        }
        for (String part : raw.split(",")) {
            if (part.trim().equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private EligibilityResult evaluateCandidates(String tenantId, CampaignEventContext event, List<Campaign> campaigns) {
        List<Campaign> qualifying = new ArrayList<>();
        List<DroppedCampaign> dropped = new ArrayList<>();

        for (Campaign campaign : campaigns) {
            if (!matchesCustomerScope(tenantId, campaign, event.customerId())) {
                dropped.add(dropped(campaign, DropReason.CUSTOMER_NOT_IN_TARGET_LIST));
                continue;
            }
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

    public boolean matchesCustomerScope(String tenantId, Campaign campaign, String customerId) {
        CustomerScope scope = campaign.getCustomerScope();
        if (scope == null || scope == CustomerScope.ALL) {
            return true;
        }
        if (customerId == null || customerId.isBlank()) {
            return false;
        }
        return targetCustomerRepository.existsByTenantIdAndCampaignUidAndCustomerId(
            tenantId,
            campaign.getCampaignUid(),
            customerId.trim()
        );
    }

    public boolean matchesTargetSegment(Campaign campaign, CampaignEventContext event) {
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
