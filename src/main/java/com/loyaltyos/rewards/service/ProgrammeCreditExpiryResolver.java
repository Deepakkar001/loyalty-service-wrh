package com.loyaltyos.rewards.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.analytics.service.TierResolver;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.TenantConfig;
import com.loyaltyos.onboarding.entity.TierDefinition;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.config.RewardEngineProperties;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Resolves {@code expires_at} for new CREDIT rows from the active programme configuration
 * ({@code expiry.model}, {@code rollingMonths}, {@code fixedDate}, tier extensions).
 * Falls back to {@link RewardEngineProperties#getDefaultCreditExpiryMonths()} when no programme
 * expiry policy is configured.
 */
@Service
public class ProgrammeCreditExpiryResolver {

    private final ProgrammeService programmeService;
    private final TenantConfigRepository tenantConfigRepository;
    private final ObjectMapper objectMapper;
    private final RewardEngineProperties rewardEngineProperties;
    private final TierResolver tierResolver;
    private final PointsLedgerRepository pointsLedgerRepository;

    public ProgrammeCreditExpiryResolver(
        ProgrammeService programmeService,
        TenantConfigRepository tenantConfigRepository,
        ObjectMapper objectMapper,
        RewardEngineProperties rewardEngineProperties,
        TierResolver tierResolver,
        PointsLedgerRepository pointsLedgerRepository
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.tenantConfigRepository = Objects.requireNonNull(tenantConfigRepository, "tenantConfigRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.rewardEngineProperties = Objects.requireNonNull(rewardEngineProperties, "rewardEngineProperties");
        this.tierResolver = Objects.requireNonNull(tierResolver, "tierResolver");
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
    }

    /**
     * Resolves expiry for a customer earn, using explicit tier uid when provided otherwise
     * inferring tier from current ledger balance.
     */
    public Instant resolveExpiresAtForCustomer(
        String tenantId,
        String programmeUid,
        String customerId,
        String explicitTierUid,
        Instant earnedAt
    ) {
        String tierUid = resolveCustomerTierUid(tenantId, programmeUid, customerId, explicitTierUid);
        return resolveExpiresAt(tenantId, programmeUid, earnedAt, tierUid);
    }

    public Instant resolveExpiresAt(String tenantId, String programmeUid, Instant earnedAt, String customerTierUid) {
        Instant at = earnedAt != null ? earnedAt : Instant.now();
        ExpiryPolicy policy = loadExpiryPolicy(tenantId, programmeUid);
        if (policy == null) {
            return fallbackRollingExpiry(at, rewardEngineProperties.getDefaultCreditExpiryMonths());
        }
        return computeExpiresAt(at, policy, customerTierUid);
    }

    private String resolveCustomerTierUid(
        String tenantId,
        String programmeUid,
        String customerId,
        String explicitTierUid
    ) {
        if (explicitTierUid != null && !explicitTierUid.isBlank()) {
            return explicitTierUid.trim();
        }
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        String programme = normalizeProgramme(programmeUid);
        BigDecimal balance = pointsLedgerRepository.sumSignedPointsForCustomer(tenantId, programme, customerId);
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        return tierResolver.resolveTierForBalance(tenantId, programme, balance)
            .map(TierDefinition::getTierUid)
            .orElse(null);
    }

    private ExpiryPolicy loadExpiryPolicy(String tenantId, String programmeUid) {
        String programme = normalizeProgramme(programmeUid);
        ExpiryPolicy fromCanonical = fromConfigJson(
            programmeService.getActiveConfigOrNull(tenantId, programme)
        );
        if (fromCanonical != null) {
            return fromCanonical;
        }
        if ("default".equals(programme)) {
            return tenantConfigRepository.findByTenantId(tenantId)
                .map(TenantConfig::getProgrammeConfig)
                .map(this::fromConfigJsonString)
                .orElse(null);
        }
        return null;
    }

    private ExpiryPolicy fromConfigJson(ProgrammeConfig cfg) {
        if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
            return null;
        }
        return fromConfigJsonString(cfg.getConfigJson());
    }

    private ExpiryPolicy fromConfigJsonString(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode expiry = root.path("expiry");
            if (expiry.isMissingNode() || expiry.isNull() || !expiry.isObject()) {
                return null;
            }
            String model = readText(expiry, "model");
            if (model == null) {
                return null;
            }
            boolean tierExtensionsEnabled = expiry.path("tierExtensionsEnabled").asBoolean(false);
            Map<String, Integer> tierExtensions = parseTierExtensions(root.path("tiers"));
            if ("ROLLING".equalsIgnoreCase(model)) {
                Integer rollingMonths = readPositiveInt(expiry, "rollingMonths");
                if (rollingMonths == null) {
                    return null;
                }
                return new ExpiryPolicy(ExpiryModel.ROLLING, rollingMonths, null, tierExtensionsEnabled, tierExtensions);
            }
            if ("FIXED_DATE".equalsIgnoreCase(model)) {
                String fixedDate = readText(expiry, "fixedDate");
                if (fixedDate == null || fixedDate.isBlank()) {
                    return null;
                }
                return new ExpiryPolicy(
                    ExpiryModel.FIXED_DATE,
                    null,
                    LocalDate.parse(fixedDate.trim()),
                    tierExtensionsEnabled,
                    tierExtensions
                );
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Integer> parseTierExtensions(JsonNode tiersRoot) {
        Map<String, Integer> out = new HashMap<>();
        if (tiersRoot == null || tiersRoot.isMissingNode() || !tiersRoot.isObject()) {
            return out;
        }
        JsonNode tiers = tiersRoot.path("tiers");
        if (!tiers.isArray()) {
            return out;
        }
        for (JsonNode tier : tiers) {
            String tierUid = readText(tier, "tierUid");
            Integer extension = readNonNegativeInt(tier, "expiryExtensionMonths");
            if (tierUid != null && extension != null && extension > 0) {
                out.put(tierUid, extension);
            }
        }
        return out;
    }

    private Instant computeExpiresAt(Instant earnedAt, ExpiryPolicy policy, String customerTierUid) {
        int extensionMonths = 0;
        if (policy.tierExtensionsEnabled()
            && customerTierUid != null
            && policy.tierExtensions().containsKey(customerTierUid)) {
            extensionMonths = policy.tierExtensions().get(customerTierUid);
        }

        if (policy.model() == ExpiryModel.FIXED_DATE) {
            LocalDate expiryDate = policy.fixedDate();
            if (extensionMonths > 0) {
                expiryDate = expiryDate.plusMonths(extensionMonths);
            }
            return expiryDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        }

        int rollingMonths = policy.rollingMonths() != null ? policy.rollingMonths() : 0;
        int totalMonths = rollingMonths + extensionMonths;
        if (totalMonths <= 0) {
            return null;
        }
        return earnedAt.atZone(ZoneOffset.UTC).plusMonths(totalMonths).toInstant();
    }

    private Instant fallbackRollingExpiry(Instant earnedAt, int months) {
        if (months <= 0) {
            return null;
        }
        return earnedAt.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }

    private static String readText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static Integer readPositiveInt(JsonNode node, String field) {
        Integer value = readNonNegativeInt(node, field);
        return value != null && value > 0 ? value : null;
    }

    private static Integer readNonNegativeInt(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            return null;
        }
        int n = value.intValue();
        return n < 0 ? null : n;
    }

    private enum ExpiryModel {
        ROLLING,
        FIXED_DATE
    }

    private record ExpiryPolicy(
        ExpiryModel model,
        Integer rollingMonths,
        LocalDate fixedDate,
        boolean tierExtensionsEnabled,
        Map<String, Integer> tierExtensions
    ) {}
}
