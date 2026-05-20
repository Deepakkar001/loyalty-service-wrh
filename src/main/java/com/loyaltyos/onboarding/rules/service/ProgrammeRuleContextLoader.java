package com.loyaltyos.onboarding.rules.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.onboarding.domain.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.rules.enums.RuleType;
import com.loyaltyos.onboarding.service.EventSchemaJsonSupport;
import com.loyaltyos.onboarding.service.ProgrammeService;
import java.util.Objects;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProgrammeRuleContextLoader {

    private final ProgrammeService programmeService;
    private final TierDefinitionRepository tierDefinitionRepository;
    private final CampaignRepository campaignRepository;
    private final ObjectMapper objectMapper;

    public ProgrammeRuleContextLoader(
        ProgrammeService programmeService,
        TierDefinitionRepository tierDefinitionRepository,
        CampaignRepository campaignRepository,
        ObjectMapper objectMapper
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.tierDefinitionRepository = Objects.requireNonNull(tierDefinitionRepository, "tierDefinitionRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Allowed {@code event.*} property names for rule validation / evaluation.
     * Campaign rules use per-campaign schema for the rule trigger; programme rules use programme-wide union.
     */
    public Set<String> resolveAllowedEventPropertyNames(
        String tenantId,
        RuleType ruleType,
        String campaignUid,
        String programmeUid,
        String triggerEventType
    ) {
        if (ruleType == RuleType.CAMPAIGN && campaignUid != null && !campaignUid.isBlank()) {
            return campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid.trim())
                .map(c -> allowlistForCampaign(c, triggerEventType))
                .orElse(Set.of());
        }
        try {
            ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
            if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
                return Set.of();
            }
            JsonNode root = objectMapper.readTree(cfg.getConfigJson());
            return extractEventFieldAllowlist(root);
        } catch (Exception e) {
            return Set.of();
        }
    }

    private Set<String> allowlistForCampaign(Campaign campaign, String triggerEventType) {
        JsonNode schema = campaign.getEventSchema();
        if (schema != null && !schema.isNull() && !schema.isMissingNode()) {
            Set<String> strict = EventSchemaJsonSupport.extractEventFieldAllowlistForTrigger(schema, triggerEventType);
            if (!strict.isEmpty()) {
                return strict;
            }
        }
        String prog = campaign.getProgrammeUid() != null && !campaign.getProgrammeUid().isBlank()
            ? campaign.getProgrammeUid()
            : "default";
        try {
            ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(campaign.getTenantId(), prog);
            if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
                return Set.of();
            }
            JsonNode root = objectMapper.readTree(cfg.getConfigJson());
            return EventSchemaJsonSupport.extractEventFieldAllowlistForTrigger(root.path("eventSchema"), triggerEventType);
        } catch (Exception e) {
            return Set.of();
        }
    }

    public ProgrammeEvaluationContext load(String tenantId, String programmeUid, String customerTierUid) {
        ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
        if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
            return ProgrammeEvaluationContext.builder()
                .basePointsRate(BigDecimal.ZERO)
                .pointsMonetaryValue(new BigDecimal("0.01"))
                .dailyCap(null)
                .monthlyCap(null)
                .conflictDefaultStrategy("BEST_FOR_CUSTOMER")
                .allowRuleOverride(true)
                .resolvedTierMultiplier(BigDecimal.ONE)
                .allowedEventPropertyNames(Set.of())
                .build();
        }
        try {
            JsonNode root = objectMapper.readTree(cfg.getConfigJson());
            BigDecimal baseRate = decimal(root.path("pointsEconomics").path("basePointsRate"), BigDecimal.ZERO);
            BigDecimal pmv = decimal(root.path("pointsEconomics").path("pointsMonetaryValue"), new BigDecimal("0.01"));
            JsonNode caps = root.path("pointsEconomics").path("caps");
            BigDecimal daily = caps.path("daily").isNumber() ? decimal(caps.path("daily"), null) : null;
            if (caps.path("daily").isNull()) {
                daily = null;
            }
            BigDecimal monthly = caps.path("monthly").isNumber() ? decimal(caps.path("monthly"), null) : null;
            if (caps.path("monthly").isNull()) {
                monthly = null;
            }
            JsonNode conflict = root.path("conflictPolicy");
            String strategy = conflict.path("defaultStrategy").asText("BEST_FOR_CUSTOMER");
            boolean allowOverride = conflict.path("allowRuleOverride").asBoolean(true);
            BigDecimal mult = resolveTierMultiplier(root, tenantId, programmeUid, customerTierUid);
            Set<String> eventFields = extractEventFieldAllowlist(root);
            return ProgrammeEvaluationContext.builder()
                .basePointsRate(baseRate)
                .pointsMonetaryValue(pmv)
                .dailyCap(daily)
                .monthlyCap(monthly)
                .conflictDefaultStrategy(strategy)
                .allowRuleOverride(allowOverride)
                .resolvedTierMultiplier(mult)
                .allowedEventPropertyNames(eventFields)
                .build();
        } catch (Exception e) {
            return ProgrammeEvaluationContext.builder()
                .basePointsRate(BigDecimal.ZERO)
                .pointsMonetaryValue(new BigDecimal("0.01"))
                .dailyCap(null)
                .monthlyCap(null)
                .conflictDefaultStrategy("BEST_FOR_CUSTOMER")
                .allowRuleOverride(true)
                .resolvedTierMultiplier(BigDecimal.ONE)
                .allowedEventPropertyNames(Set.of())
                .build();
        }
    }

    /**
     * Property names allowed as the first segment under {@code event.*} (canonical programme eventSchema + core API fields).
     * Non-empty set enforces conditions to reference only declared / core event fields.
     */
    public static Set<String> extractEventFieldAllowlist(JsonNode programmeRoot) {
        Set<String> names = new LinkedHashSet<>();
        names.add("amount");
        names.add("eventType");
        names.add("channel");
        names.add("merchantId");
        names.add("transactionId");
        names.add("timestamp");
        names.add("customerId");
        JsonNode es = programmeRoot.path("eventSchema");
        JsonNode defs = es.path("eventDefinitions");
        if (defs.isArray()) {
            for (JsonNode def : defs) {
                for (JsonNode sf : def.path("coreFields")) {
                    String n = sf.path("name").asText(null);
                    if (n != null && !n.isBlank()) {
                        names.add(n);
                    }
                }
            }
        }
        for (JsonNode sf : es.path("standardFields")) {
            String n = sf.path("name").asText(null);
            if (n != null && !n.isBlank()) {
                names.add(n);
            }
        }
        for (JsonNode cf : es.path("customFields")) {
            String n = cf.path("name").asText(null);
            if (n != null && !n.isBlank()) {
                names.add(n);
            }
        }
        return Set.copyOf(names);
    }

    private BigDecimal resolveTierMultiplier(JsonNode root, String tenantId, String programmeUid, String customerTierUid) {
        JsonNode tiersBlock = root.path("tiers");
        if (!tiersBlock.path("enabled").asBoolean(false)) {
            return BigDecimal.ONE;
        }
        JsonNode tiersArr = tiersBlock.path("tiers");
        if (customerTierUid != null && !customerTierUid.isBlank()) {
            Optional<BigDecimal> fromJson = findMultiplierInJson(tiersArr, customerTierUid);
            if (fromJson.isPresent()) {
                return fromJson.get();
            }
            List<TierDefinition> dbTiers = tierDefinitionRepository
                .findByTenantIdAndProgrammeUidOrderByRankOrderAsc(tenantId, programmeUid);
            for (TierDefinition td : dbTiers) {
                if (customerTierUid.equals(td.getTierUid())) {
                    return td.getPointsMultiplier() != null ? td.getPointsMultiplier() : BigDecimal.ONE;
                }
            }
        }
        if (tiersArr.isArray() && !tiersArr.isEmpty()) {
            JsonNode first = tiersArr.get(0);
            return decimal(first.path("multiplier"), BigDecimal.ONE);
        }
        List<TierDefinition> dbTiers = tierDefinitionRepository
            .findByTenantIdAndProgrammeUidOrderByRankOrderAsc(tenantId, programmeUid);
        if (!dbTiers.isEmpty()) {
            TierDefinition td = dbTiers.getFirst();
            return td.getPointsMultiplier() != null ? td.getPointsMultiplier() : BigDecimal.ONE;
        }
        return BigDecimal.ONE;
    }

    private Optional<BigDecimal> findMultiplierInJson(JsonNode tiersArr, String tierUid) {
        if (!tiersArr.isArray()) {
            return Optional.empty();
        }
        for (JsonNode t : tiersArr) {
            if (tierUid.equals(t.path("tierUid").asText())) {
                return Optional.of(decimal(t.path("multiplier"), BigDecimal.ONE));
            }
        }
        return Optional.empty();
    }

    private static BigDecimal decimal(JsonNode n, BigDecimal defaultVal) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return defaultVal;
        }
        if (n.isNumber()) {
            return n.decimalValue();
        }
        try {
            return new BigDecimal(n.asText());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
