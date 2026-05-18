package com.loyaltyos.onboarding.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.TenantConfig;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import com.loyaltyos.onboarding.domain.entity.WebhookSubscription;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.TierThresholdType;
import com.loyaltyos.onboarding.dto.request.ProgrammeConfigRequest;
import com.loyaltyos.onboarding.dto.response.ProgrammeConfigResponse;
// import com.loyaltyos.onboarding.event.ProgrammeConfigUpdatedEvent; // with Kafka publish
import com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.repository.WebhookSubscriptionRepository;
import com.loyaltyos.onboarding.rules.service.RuleCacheService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
// import org.springframework.kafka.core.KafkaTemplate; // re-enable with Kafka
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class TenantConfigService {

    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final TierDefinitionRepository tierDefinitionRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final ProgrammeRepository programmeRepository;
    private final ProgrammeConfigRepository programmeConfigRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final OnboardingStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final ProgrammeConfigSchemaValidator programmeConfigSchemaValidator;
    private final RuleCacheService ruleCacheService;
    // private final KafkaTemplate<String, Object> kafkaTemplate; // re-enable with Kafka

    public TenantConfigService(
        TenantOnboardingRepository tenantOnboardingRepository,
        TenantConfigRepository tenantConfigRepository,
        TierDefinitionRepository tierDefinitionRepository,
        WebhookSubscriptionRepository webhookSubscriptionRepository,
        ProgrammeRepository programmeRepository,
        ProgrammeConfigRepository programmeConfigRepository,
        OnboardingAuditLogRepository auditLogRepository,
        OnboardingStateMachine stateMachine,
        ObjectMapper objectMapper,
        ProgrammeConfigSchemaValidator programmeConfigSchemaValidator,
        RuleCacheService ruleCacheService
    ) {
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.tenantConfigRepository = Objects.requireNonNull(tenantConfigRepository, "tenantConfigRepository");
        this.tierDefinitionRepository = Objects.requireNonNull(tierDefinitionRepository, "tierDefinitionRepository");
        this.webhookSubscriptionRepository = Objects.requireNonNull(webhookSubscriptionRepository, "webhookSubscriptionRepository");
        this.programmeRepository = Objects.requireNonNull(programmeRepository, "programmeRepository");
        this.programmeConfigRepository = Objects.requireNonNull(programmeConfigRepository, "programmeConfigRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.programmeConfigSchemaValidator = Objects.requireNonNull(programmeConfigSchemaValidator, "programmeConfigSchemaValidator");
        this.ruleCacheService = Objects.requireNonNull(ruleCacheService, "ruleCacheService");
    }

    @Transactional
    public ProgrammeConfigResponse saveLegacyProgrammeConfiguration(String tenantId, ProgrammeConfigRequest request) {
        TenantOnboarding tenant = tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        validateTierStructure(request);

        TenantConfig cfg = tenantConfigRepository.findByTenantId(tenantId).orElse(null);
        if (cfg == null) {
            cfg = TenantConfig.builder()
                .tenantId(tenantId)
                .displayName(request.getProgrammeName().trim())
                .subscriptionTier(tenant.getSubscriptionTier())
                .dataResidencyRegion(tenant.getDataResidencyRegion())
                .ingestionModes(tenant.getIdentityMode())
                .build();
        } else {
            cfg.setDisplayName(request.getProgrammeName().trim());
        }

        // Store legacy programme settings into feature_flags JSON to avoid schema drift.
        Map<String, Object> programme = new LinkedHashMap<>();
        programme.put("programmeName", request.getProgrammeName());
        programme.put("pointsName", request.getPointsName());
        programme.put("pointsSymbol", request.getPointsSymbol());
        programme.put("baseCurrency", request.getBaseCurrency());
        programme.put("basePointsRate", request.getBasePointsRate());
        programme.put("minRedemptionPoints", request.getMinRedemptionPoints());
        programme.put("maxRedemptionPctPerTxn", request.getMaxRedemptionPctPerTxn());

        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("tiersEnabled", Boolean.TRUE.equals(request.getTiersEnabled()));
        flags.put("programme", programme);
        if (request.getNotificationPreferences() != null) {
            flags.put("notificationPreferences", request.getNotificationPreferences());
        }
        cfg.setFeatureFlags(toJson(flags));

        // Keep webhook config summary in tenant_config too (for fast reads).
        cfg.setWebhookConfig(toJson(request.getWebhookConfig()));

        tenantConfigRepository.save(cfg);

        // Full replace tiers for tenant (onboarding simplicity)
        tierDefinitionRepository.deleteByTenantId(tenantId);
        if (Boolean.TRUE.equals(request.getTiersEnabled())) {
            List<TierDefinition> tiers = new ArrayList<>();
            for (ProgrammeConfigRequest.TierRequest t : request.getTiers()) {
                tiers.add(TierDefinition.builder()
                    .tenantId(tenantId)
                    .programmeUid("default")
                    .tierUid(UUID.randomUUID().toString())
                    .name(t.getName().trim())
                    .rankOrder(t.getRank())
                    .entryThreshold(t.getMinPoints())
                    .maintenanceThreshold(t.getMinPoints())
                    .thresholdType(TierThresholdType.LIFETIME_POINTS)
                    .pointsMultiplier(t.getMultiplier())
                    .benefits(t.getBenefits() == null ? null : toJson(t.getBenefits()))
                    .build());
            }
            tierDefinitionRepository.saveAll(tiers);
        }

        // Canonical programme config blob + schema validation (legacy => programmeId = "default")
        List<TierDefinition> persistedTiers = tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc(tenantId);
        JsonNode canonical = objectMapper.valueToTree(buildCanonicalProgrammeConfig(cfg, request, persistedTiers));
        programmeConfigSchemaValidator.validate(canonical);
        cfg.setProgrammeConfig(toJson(canonical));
        cfg.setProgrammeConfigVersion((cfg.getProgrammeConfigVersion() == null ? 0 : cfg.getProgrammeConfigVersion()) + 1);
        tenantConfigRepository.save(cfg);

        // Also write into programme_config table for v2 consumers (default programme).
        ensureDefaultProgrammeExists(tenantId, request.getProgrammeName());
        // IMPORTANT: keep Programme.activeConfigVersion in sync with programme_config writes.
        // Otherwise, v2 saveConfig() will compute nextVersion incorrectly and can collide on (tenantId, programmeUid, version).
        final Integer newVersion = cfg.getProgrammeConfigVersion();
        programmeRepository.findByTenantIdAndProgrammeUid(tenantId, "default").ifPresent(p -> {
            p.setActiveConfigVersion(newVersion);
            programmeRepository.save(p);
        });
        com.loyaltyos.onboarding.domain.entity.ProgrammeConfig pc = com.loyaltyos.onboarding.domain.entity.ProgrammeConfig.builder()
            .tenantId(tenantId)
            .programmeUid("default")
            .configVersion(cfg.getProgrammeConfigVersion())
            .configJson(toJson(canonical))
            .effectiveFrom(Instant.now())
            .createdByActorId(tenantId)
            .createdByRole("TENANT")
            .build();
        programmeConfigRepository.save(Objects.requireNonNull(pc, "programmeConfig"));

        ruleCacheService.invalidateProgramme(tenantId, "default");

        // Webhook subscription (normalized)
        String endpointUrl = null;
        if (request.getWebhookConfig() != null) {
            Object url = request.getWebhookConfig().get("sandboxEndpointUrl");
            if (url != null && !url.toString().isBlank()) endpointUrl = url.toString().trim();
        }
        if (endpointUrl != null && !endpointUrl.isBlank()) {
            WebhookSubscription sub = webhookSubscriptionRepository
                .findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .orElse(null);
            if (sub == null) {
                sub = WebhookSubscription.builder()
                    .tenantId(tenantId)
                    .endpointUrl(endpointUrl)
                    .secretVaultRef("dev://tenants/" + tenantId + "/webhook-secret")
                    .eventsSubscribed("[]")
                    .active(true)
                    .build();
            } else {
                sub.setEndpointUrl(endpointUrl);
                sub.setActive(true);
            }
            webhookSubscriptionRepository.save(sub);
        }

        // Transition onboarding status to CONFIGURED
        if (tenant.getOnboardingStatus() != OnboardingStatus.CONFIGURED) {
            stateMachine.transition(tenant, OnboardingStatus.CONFIGURED, tenantId, "TENANT");
            tenantOnboardingRepository.save(tenant);
        }

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROGRAMME_CONFIGURATION_SAVED")
            .actorId(tenantId)
            .actorRole("TENANT")
            .afterState(Map.of("programmeName", request.getProgrammeName()))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        // --- Kafka publish (disabled) — topic platform.config.updates ---
        // ProgrammeConfigUpdatedEvent event = ProgrammeConfigUpdatedEvent.builder()
        //     .tenantId(tenantId)
        //     .programmeId("default")
        //     .configVersion(cfg.getProgrammeConfigVersion())
        //     .changedSections(List.of("programmeIdentity", "pointsEconomics", "tiers", "webhooks", "expiry", "eventSchema", "conflictPolicy"))
        //     .changedAt(Instant.now())
        //     .changedByActorId(tenantId)
        //     .changedByActorRole("TENANT")
        //     .build();
        // kafkaTemplate.send("platform.config.updates", tenantId, event);

        return buildLegacyResponse(tenantId, request);
    }

    private void ensureDefaultProgrammeExists(String tenantId, String programmeName) {
        if (programmeRepository.existsByTenantIdAndProgrammeUid(tenantId, "default")) return;
        com.loyaltyos.onboarding.domain.entity.Programme p = com.loyaltyos.onboarding.domain.entity.Programme.builder()
            .tenantId(tenantId)
            .programmeUid("default")
            .name(programmeName == null ? "Default Programme" : programmeName.trim())
            .status(com.loyaltyos.onboarding.domain.entity.Programme.ProgrammeStatus.DRAFT)
            .activeConfigVersion(0)
            .build();
        programmeRepository.save(Objects.requireNonNull(p, "programme"));
    }

    private Map<String, Object> buildCanonicalProgrammeConfig(
        TenantConfig cfg,
        ProgrammeConfigRequest request,
        List<TierDefinition> persistedTiers
    ) {
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("programmeIdentity", Map.of(
            "programmeName", request.getProgrammeName(),
            "pointsName", request.getPointsName(),
            "pointsSymbol", request.getPointsSymbol(),
            "baseCurrency", request.getBaseCurrency()
        ));

        Map<String, Object> pointsEconomics = new LinkedHashMap<>();
        pointsEconomics.put("pointsMonetaryValue", cfg.getPointsCurrencyRate() == null ? 0.01 : cfg.getPointsCurrencyRate());
        pointsEconomics.put("basePointsRate", request.getBasePointsRate());
        Map<String, Object> caps = new LinkedHashMap<>();
        caps.put("daily", cfg.getDailyPointsCap());
        caps.put("monthly", null);
        pointsEconomics.put("caps", caps);
        Map<String, Object> welcomeBonus = new LinkedHashMap<>();
        welcomeBonus.put("enabled", false);
        welcomeBonus.put("amount", 0);
        welcomeBonus.put("tierOverrides", List.of());
        pointsEconomics.put("welcomeBonus", welcomeBonus);
        root.put("pointsEconomics", pointsEconomics);

        root.put("conflictPolicy", Map.of(
            "defaultStrategy", "BEST_FOR_CUSTOMER",
            "allowRuleOverride", true
        ));

        List<Map<String, Object>> tierItems = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getTiersEnabled()) && persistedTiers != null && !persistedTiers.isEmpty()) {
            for (TierDefinition t : persistedTiers) {
                Map<String, Object> tier = new LinkedHashMap<>();
                tier.put("tierUid", t.getTierUid());
                tier.put("name", t.getName());
                tier.put("rank", t.getRankOrder());
                tier.put("entryThreshold", t.getEntryThreshold());
                tier.put("maintenanceThreshold", t.getMaintenanceThreshold());
                tier.put("multiplier", t.getPointsMultiplier());
                tier.put("expiryExtensionMonths", null);
                tierItems.add(tier);
            }
        } else {
            Map<String, Object> tier = new LinkedHashMap<>();
            tier.put("tierUid", "standard");
            tier.put("name", "Standard");
            tier.put("rank", 1);
            tier.put("entryThreshold", 0);
            tier.put("maintenanceThreshold", 0);
            tier.put("multiplier", 1);
            tier.put("expiryExtensionMonths", null);
            tierItems.add(tier);
        }

        root.put("tiers", Map.of(
            "enabled", Boolean.TRUE.equals(request.getTiersEnabled()),
            "thresholdType", "LIFETIME_POINTS",
            "reviewCycle", "ANNUAL",
            "gracePeriodDays", 90,
            "downgradeWarningDays", 60,
            "tiers", tierItems
        ));

        Map<String, Object> breakage = new LinkedHashMap<>();
        breakage.put("enabled", true);
        breakage.put("reportFrequency", "MONTHLY");
        breakage.put("accountingCutoffTimezone", "Asia/Kolkata");
        breakage.put("exportEnabled", true);
        breakage.put("exportFormats", List.of("CSV"));
        breakage.put("includeTierBreakdown", true);
        breakage.put("includeProgrammeBreakdown", true);

        Map<String, Object> expiry = new LinkedHashMap<>();
        expiry.put("model", "ROLLING");
        expiry.put("rollingMonths", 24);
        expiry.put("fixedDate", null);
        expiry.put("tierExtensionsEnabled", true);
        expiry.put("notificationScheduleDays", List.of(60, 7, 1));
        expiry.put("processMode", "OVERNIGHT_BATCH");
        expiry.put("breakage", breakage);
        root.put("expiry", expiry);

        root.put("eventSchema", Map.of(
            "version", 1,
            "standardFields", List.of(
                Map.of("name", "eventType", "type", "string", "required", true),
                Map.of("name", "amount", "type", "number", "required", true),
                Map.of("name", "transactionId", "type", "string", "required", true),
                Map.of("name", "timestamp", "type", "date-time", "required", true),
                Map.of("name", "customerId", "type", "string", "required", true)
            ),
            "customFields", List.of(),
            "backwardCompatibilityDays", 30
        ));

        String sandboxEndpointUrl = "";
        if (request.getWebhookConfig() != null) {
            Object url = request.getWebhookConfig().get("sandboxEndpointUrl");
            if (url != null) sandboxEndpointUrl = String.valueOf(url);
        }
        root.put("webhooks", Map.of(
            "sandboxEndpointUrl", sandboxEndpointUrl,
            "subscribedEvents", List.of()
        ));

        return root;
    }

    @Transactional(readOnly = true)
    public ProgrammeConfigResponse getLegacyProgrammeConfiguration(String tenantId) {
        TenantConfig cfg = tenantConfigRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        Map<String, Object> featureFlags = readJsonMap(cfg.getFeatureFlags());
        Map<String, Object> programme = featureFlags == null ? null : safeMap(featureFlags.get("programme"));
        String programmeName = programme == null ? cfg.getDisplayName() : String.valueOf(programme.getOrDefault("programmeName", cfg.getDisplayName()));
        String pointsName = programme == null ? null : asString(programme.get("pointsName"));
        String pointsSymbol = programme == null ? null : asString(programme.get("pointsSymbol"));
        String baseCurrency = programme == null ? null : asString(programme.get("baseCurrency"));
        String basePointsRate = programme == null ? null : String.valueOf(programme.get("basePointsRate"));
        String minRedemptionPoints = programme == null ? null : String.valueOf(programme.get("minRedemptionPoints"));
        String maxRedemptionPctPerTxn = programme == null ? null : String.valueOf(programme.get("maxRedemptionPctPerTxn"));
        Boolean tiersEnabled = featureFlags == null ? null : Boolean.valueOf(String.valueOf(featureFlags.getOrDefault("tiersEnabled", "true")));

        List<TierDefinition> tiers = tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc(tenantId);
        List<ProgrammeConfigResponse.Tier> tierDtos = tiers.stream().map(t ->
            ProgrammeConfigResponse.Tier.builder()
                .tierUid(t.getTierUid())
                .name(t.getName())
                .rank(t.getRankOrder())
                .entryThreshold(t.getEntryThreshold() == null ? null : t.getEntryThreshold().toPlainString())
                .maintenanceThreshold(t.getMaintenanceThreshold() == null ? null : t.getMaintenanceThreshold().toPlainString())
                .thresholdType(t.getThresholdType() == null ? null : t.getThresholdType().name())
                .multiplier(t.getPointsMultiplier() == null ? null : t.getPointsMultiplier().toPlainString())
                .benefits(readJsonListString(t.getBenefits()))
                .build()
        ).toList();

        return ProgrammeConfigResponse.builder()
            .tenantId(tenantId)
            .programmeName(programmeName)
            .pointsName(pointsName)
            .pointsSymbol(pointsSymbol)
            .baseCurrency(baseCurrency)
            .basePointsRate(basePointsRate)
            .minRedemptionPoints(minRedemptionPoints)
            .maxRedemptionPctPerTxn(maxRedemptionPctPerTxn)
            .tiersEnabled(tiersEnabled)
            .tiers(tierDtos)
            .webhookConfig(readJsonMap(cfg.getWebhookConfig()))
            .build();
    }

    private ProgrammeConfigResponse buildLegacyResponse(String tenantId, ProgrammeConfigRequest request) {
        List<TierDefinition> tiers = tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc(tenantId);
        List<ProgrammeConfigResponse.Tier> tierDtos = tiers.stream().map(t ->
            ProgrammeConfigResponse.Tier.builder()
                .tierUid(t.getTierUid())
                .name(t.getName())
                .rank(t.getRankOrder())
                .entryThreshold(t.getEntryThreshold() == null ? null : t.getEntryThreshold().toPlainString())
                .maintenanceThreshold(t.getMaintenanceThreshold() == null ? null : t.getMaintenanceThreshold().toPlainString())
                .thresholdType(t.getThresholdType() == null ? null : t.getThresholdType().name())
                .multiplier(t.getPointsMultiplier() == null ? null : t.getPointsMultiplier().toPlainString())
                .benefits(readJsonListString(t.getBenefits()))
                .build()
        ).toList();

        return ProgrammeConfigResponse.builder()
            .tenantId(tenantId)
            .programmeName(request.getProgrammeName())
            .pointsName(request.getPointsName())
            .pointsSymbol(request.getPointsSymbol())
            .baseCurrency(request.getBaseCurrency())
            .basePointsRate(request.getBasePointsRate().toPlainString())
            .minRedemptionPoints(request.getMinRedemptionPoints().toPlainString())
            .maxRedemptionPctPerTxn(request.getMaxRedemptionPctPerTxn().toPlainString())
            .tiersEnabled(request.getTiersEnabled())
            .tiers(tierDtos)
            .webhookConfig(request.getWebhookConfig())
            .build();
    }

    private void validateTierStructure(ProgrammeConfigRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.getTiersEnabled() == null) {
            errors.put("tiersEnabled", "tiersEnabled is required");
        }

        List<ProgrammeConfigRequest.TierRequest> tiers = request.getTiers();
        if (Boolean.TRUE.equals(request.getTiersEnabled())) {
            if (tiers == null || tiers.isEmpty()) {
                errors.put("tiers", "At least 1 tier is required when tiers are enabled");
            } else if (tiers.size() > 10) {
                errors.put("tiers", "Maximum 10 tiers allowed");
            }
        }

        if (tiers != null && !tiers.isEmpty()) {
            // Sort by rank to validate sequentiality and ascending thresholds
            tiers = tiers.stream().sorted((a, b) -> Integer.compare(a.getRank(), b.getRank())).toList();

            int expectedRank = 1;
            BigDecimal prevMin = null;
            for (int i = 0; i < tiers.size(); i++) {
                var t = tiers.get(i);
                if (t.getRank() == null) {
                    errors.put("tiers[" + i + "].rank", "rank is required");
                    continue;
                }
                if (t.getRank() != expectedRank) {
                    errors.put("tiers[" + i + "].rank", "rank must be sequential starting from 1");
                }
                expectedRank++;

                if (t.getMinPoints() == null || t.getMinPoints().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("tiers[" + i + "].minPoints", "minPoints must be >= 0");
                } else if (prevMin != null && t.getMinPoints().compareTo(prevMin) <= 0) {
                    errors.put("tiers[" + i + "].minPoints", "minPoints must be ascending with tier rank");
                } else {
                    prevMin = t.getMinPoints();
                }

                if (t.getMultiplier() == null || t.getMultiplier().compareTo(BigDecimal.ONE) < 0) {
                    errors.put("tiers[" + i + "].multiplier", "multiplier must be >= 1.0");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ProgrammeConfigValidationException("Programme configuration validation failed", errors);
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readJsonListString(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Object v = objectMapper.readValue(json, Object.class);
            if (v instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) out.add(String.valueOf(o));
                return out;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> safeMap(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (var e : m.entrySet()) {
                if (e.getKey() != null) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return out;
        }
        return null;
    }

    private String asString(Object v) {
        return v == null ? null : v.toString();
    }
}

