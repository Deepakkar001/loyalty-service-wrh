package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.util.TriggerEventTypes;
import com.loyaltyos.campaigns.model.CampaignAwardType;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import com.loyaltyos.onboarding.domain.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CampaignProgrammeValidator {

    private final ProgrammeRepository programmeRepository;
    private final ProgrammeService programmeService;
    private final TierDefinitionRepository tierDefinitionRepository;
    private final ObjectMapper objectMapper;

    public CampaignProgrammeValidator(
        ProgrammeRepository programmeRepository,
        ProgrammeService programmeService,
        TierDefinitionRepository tierDefinitionRepository,
        ObjectMapper objectMapper
    ) {
        this.programmeRepository = Objects.requireNonNull(programmeRepository, "programmeRepository");
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.tierDefinitionRepository = Objects.requireNonNull(tierDefinitionRepository, "tierDefinitionRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String requireProgrammeUid(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            throw new CampaignBadRequestException("programmeUid is required and cannot be blank");
        }
        return programmeUid.trim();
    }

    public void assertProgrammeExists(String tenantId, String programmeUid) {
        requireProgrammeUid(programmeUid);
        if (!programmeRepository.existsByTenantIdAndProgrammeUid(tenantId, programmeUid)) {
            throw new CampaignBadRequestException("Programme not found: " + programmeUid);
        }
    }

  /** Tenant-defined event types (comma-separated) — not validated against programme event schema. */
    public void validateTriggerEventType(String tenantId, String programmeUid, String triggerEventType) {
        try {
            TriggerEventTypes.validateSerialized(triggerEventType);
        } catch (IllegalArgumentException e) {
            throw new CampaignBadRequestException(e.getMessage());
        }
    }

    /** Allows empty trigger list on create/update; event types are added via rules or event schema. */
    public void validateTriggerEventTypeIfPresent(String tenantId, String programmeUid, String triggerEventType) {
        if (triggerEventType == null || triggerEventType.isBlank()) {
            return;
        }
        validateTriggerEventType(tenantId, programmeUid, triggerEventType);
    }

    /** Tier UIDs must exist in tenant {@code tier_definitions}. */
    public void validateTierUids(String tenantId, String programmeUid, CampaignTargetSegment segment) {
        if (segment == null || segment.tierUids() == null || segment.tierUids().isEmpty()) {
            return;
        }
        Set<String> valid = resolveAllowedTierUids(tenantId);
        if (valid.isEmpty()) {
            throw new CampaignBadRequestException(
                "No tier definitions found for this tenant. Configure tiers before using tier targeting."
            );
        }
        for (String uid : segment.tierUids()) {
            if (uid == null || uid.isBlank()) {
                continue;
            }
            if (!valid.contains(uid.trim())) {
                throw new CampaignBadRequestException("Unknown tierUid: " + uid);
            }
        }
    }

    public void validateOfferConfig(CampaignOfferConfig offer) {
        if (offer == null || offer.awardType() == null || offer.awardType().isBlank()) {
            throw new CampaignBadRequestException("offerConfig.awardType is required");
        }
        String type = offer.awardType().trim();
        switch (type) {
            case CampaignAwardType.POINTS_BONUS -> {
                if (offer.bonusPoints() == null || offer.bonusPoints().signum() <= 0) {
                    throw new CampaignBadRequestException("bonusPoints must be positive for POINTS_BONUS");
                }
            }
            case CampaignAwardType.MULTIPLIER_ON_RULE_POINTS -> {
                if (offer.multiplierOnRulePoints() == null || offer.multiplierOnRulePoints().compareTo(BigDecimal.ONE) <= 0) {
                    throw new CampaignBadRequestException("multiplierOnRulePoints must be greater than 1 for MULTIPLIER_ON_RULE_POINTS");
                }
            }
            case CampaignAwardType.FLAT_CASHBACK -> {
                if (offer.cashbackValue() == null || offer.cashbackValue().signum() <= 0) {
                    throw new CampaignBadRequestException("cashbackValue must be positive for FLAT_CASHBACK");
                }
            }
            case CampaignAwardType.PERCENT_CASHBACK -> {
                if (offer.cashbackValue() == null || offer.cashbackValue().signum() <= 0) {
                    throw new CampaignBadRequestException("cashbackValue must be positive for PERCENT_CASHBACK");
                }
            }
            default -> throw new CampaignBadRequestException("Unknown awardType: " + type);
        }
    }

    /**
     * Resolves tier rows for campaign validation. Programme-scoped rows are preferred; legacy onboarding
     * may persist tiers with a null programme_uid (treated as tenant-wide for the default programme).
     */
    Set<String> resolveAllowedTierUids(String tenantId) {
        Set<String> valid = new HashSet<>();
        for (TierDefinition td : tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc(tenantId)) {
            if (td.getTierUid() != null && !td.getTierUid().isBlank()) {
                valid.add(td.getTierUid().trim());
            }
        }
        return valid;
    }

    List<TierDefinition> resolveTierDefinitions(String tenantId, String programmeUid) {
        String normalizedProgramme = requireProgrammeUid(programmeUid);
        List<TierDefinition> scoped = tierDefinitionRepository.findByTenantIdAndProgrammeUidOrderByRankOrderAsc(
            tenantId,
            normalizedProgramme
        );
        if (!scoped.isEmpty()) {
            return scoped;
        }
        return tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc(tenantId).stream()
            .filter(td -> tierAppliesToProgramme(td.getProgrammeUid(), normalizedProgramme))
            .toList();
    }

    private Set<String> extractConfiguredTierUidsFromProgramme(String tenantId, String programmeUid) {
        ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
        if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
            return Set.of();
        }
        try {
            JsonNode root = objectMapper.readTree(cfg.getConfigJson());
            return extractConfiguredTierUids(root);
        } catch (Exception e) {
            return Set.of();
        }
    }

    static Set<String> extractConfiguredTierUids(JsonNode programmeRoot) {
        Set<String> uids = new HashSet<>();
        JsonNode tiersBlock = programmeRoot.path("tiers");
        if (!tiersBlock.path("enabled").asBoolean(true) && tiersBlock.has("enabled")) {
            return uids;
        }
        JsonNode tiersArr = tiersBlock.path("tiers");
        if (tiersArr.isArray()) {
            for (JsonNode t : tiersArr) {
                String uid = t.path("tierUid").asText(null);
                if (uid != null && !uid.isBlank()) {
                    uids.add(uid.trim());
                }
            }
        }
        return uids;
    }

    private static boolean tierAppliesToProgramme(String tierProgrammeUid, String programmeUid) {
        if (tierProgrammeUid == null || tierProgrammeUid.isBlank()) {
            return "default".equals(programmeUid);
        }
        return programmeUid.equals(tierProgrammeUid.trim());
    }

    static Set<String> extractConfiguredEventTypes(JsonNode programmeRoot) {
        Set<String> types = new HashSet<>();
        JsonNode es = programmeRoot.path("eventSchema");
        if (es.isMissingNode() || es.isNull()) {
            return types;
        }
        JsonNode defs = es.path("eventDefinitions");
        if (defs.isArray()) {
            for (JsonNode def : defs) {
                String et = def.path("eventType").asText(null);
                if (et != null && !et.isBlank()) {
                    types.add(et.trim());
                }
            }
        }
        if (types.isEmpty()) {
            JsonNode std = es.path("standardFields");
            if (std.isArray() && !std.isEmpty()) {
                types.add("PURCHASE");
            }
        }
        return types;
    }

    private static boolean matchesConfiguredEventType(Set<String> allowed, String candidate) {
        for (String a : allowed) {
            if (a != null && a.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
