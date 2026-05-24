package com.loyaltyos.rewards.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.TenantConfig;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.dto.RedemptionLimits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class ProgrammeRedemptionConfigResolver {

    private final ProgrammeService programmeService;
    private final TenantConfigRepository tenantConfigRepository;
    private final ObjectMapper objectMapper;

    public ProgrammeRedemptionConfigResolver(
        ProgrammeService programmeService,
        TenantConfigRepository tenantConfigRepository,
        ObjectMapper objectMapper
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.tenantConfigRepository = Objects.requireNonNull(tenantConfigRepository, "tenantConfigRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public RedemptionLimits resolve(String tenantId, String programmeUid) {
        String p = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid;
        RedemptionLimits fromCanonical = fromCanonicalConfig(tenantId, p);
        if (fromCanonical.minRedemptionPoints() != null || fromCanonical.maxRedemptionPctPerTxn() != null) {
            return fromCanonical;
        }
        return fromLegacyFeatureFlags(tenantId);
    }

    private RedemptionLimits fromCanonicalConfig(String tenantId, String programmeUid) {
        ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
        if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
            return RedemptionLimits.none();
        }
        try {
            JsonNode root = objectMapper.readTree(cfg.getConfigJson());
            BigDecimal min = readDecimal(root, "minRedemptionPoints");
            BigDecimal maxPct = readDecimal(root, "maxRedemptionPctPerTxn");
            if (min == null) {
                min = readDecimal(root.path("programme"), "minRedemptionPoints");
            }
            if (maxPct == null) {
                maxPct = readDecimal(root.path("programme"), "maxRedemptionPctPerTxn");
            }
            if (min == null && maxPct == null) {
                return RedemptionLimits.none();
            }
            return new RedemptionLimits(min, maxPct);
        } catch (Exception e) {
            return RedemptionLimits.none();
        }
    }

    private RedemptionLimits fromLegacyFeatureFlags(String tenantId) {
        return tenantConfigRepository.findByTenantId(tenantId)
            .map(this::parseLegacyLimits)
            .orElse(RedemptionLimits.none());
    }

    private RedemptionLimits parseLegacyLimits(TenantConfig cfg) {
        try {
            if (cfg.getFeatureFlags() == null || cfg.getFeatureFlags().isBlank()) {
                return RedemptionLimits.none();
            }
            JsonNode flags = objectMapper.readTree(cfg.getFeatureFlags());
            JsonNode programme = flags.path("programme");
            if (programme.isMissingNode() || programme.isNull()) {
                return RedemptionLimits.none();
            }
            BigDecimal min = readDecimal(programme, "minRedemptionPoints");
            BigDecimal maxPct = readDecimal(programme, "maxRedemptionPctPerTxn");
            return new RedemptionLimits(min, maxPct);
        } catch (Exception e) {
            return RedemptionLimits.none();
        }
    }

    private static BigDecimal readDecimal(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        return parseDecimal(value.asText());
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
