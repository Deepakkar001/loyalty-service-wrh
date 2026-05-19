package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.StackMode;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.util.TriggerEventTypes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final CampaignAnalyticsService analyticsService;
    private final CampaignProgrammeValidator programmeValidator;
    private final CampaignProperties campaignProperties;
    private final ObjectMapper objectMapper;

    public CampaignService(
        CampaignRepository campaignRepository,
        CampaignAnalyticsService analyticsService,
        CampaignProgrammeValidator programmeValidator,
        CampaignProperties campaignProperties,
        ObjectMapper objectMapper
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
        this.programmeValidator = Objects.requireNonNull(programmeValidator, "programmeValidator");
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    private void assertCampaignsEnabled() {
        if (!campaignProperties.isEnabled()) {
            throw new CampaignBadRequestException("Campaign module is disabled");
        }
    }

    @Transactional
    public CampaignResponse create(String tenantId, CampaignUpsertRequest req, String actorId) {
        assertCampaignsEnabled();
        String programmeUid = defaultProgrammeUid(req.getProgrammeUid());
        programmeValidator.assertProgrammeExists(tenantId, programmeUid);
        validateUpsert(tenantId, programmeUid, req);

        String campaignUid = (req.getCampaignUid() != null && !req.getCampaignUid().isBlank())
            ? req.getCampaignUid().trim()
            : UUID.randomUUID().toString();

        if (campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid).isPresent()) {
            throw new CampaignConflictException("Campaign already exists: " + campaignUid);
        }

        Campaign entity = mapNewEntity(tenantId, programmeUid, campaignUid, req, actorId);
        warnIfBudgetExceedsThreshold(tenantId, entity.getBudgetTotal());
        Campaign saved = campaignRepository.save(entity);
        return toResponse(saved, exceedsThreshold(saved.getBudgetTotal()));
    }

    @Transactional
    public CampaignResponse update(String tenantId, String campaignUid, CampaignUpsertRequest req, String actorId) {
        assertCampaignsEnabled();
        Campaign existing = loadCampaign(tenantId, campaignUid);
        if (existing.getStatus() != CampaignStatus.DRAFT) {
            throw new CampaignConflictException("Only DRAFT campaigns can be updated");
        }

        String programmeUid = programmeValidator.requireProgrammeUid(req.getProgrammeUid());
        if (!programmeUid.equals(existing.getProgrammeUid())) {
            throw new CampaignBadRequestException("programmeUid cannot be changed on update");
        }
        programmeValidator.assertProgrammeExists(tenantId, programmeUid);
        validateUpsert(tenantId, programmeUid, req);

        applyUpsert(existing, req, actorId);
        warnIfBudgetExceedsThreshold(tenantId, existing.getBudgetTotal());
        Campaign saved = campaignRepository.save(existing);
        return toResponse(saved, exceedsThreshold(saved.getBudgetTotal()));
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(String tenantId, String campaignUid) {
        Campaign c = loadCampaign(tenantId, campaignUid);
        return toResponse(c, exceedsThreshold(c.getBudgetTotal()));
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> list(String tenantId, String programmeUid, CampaignStatus status) {
        List<Campaign> rows;
        if (programmeUid != null && !programmeUid.isBlank()) {
            String p = programmeUid.trim();
            rows = status == null
                ? campaignRepository.findByTenantIdAndProgrammeUidOrderByPriorityDescCreatedAtDesc(tenantId, p)
                : campaignRepository.findByTenantIdAndProgrammeUidAndStatusOrderByPriorityDescCreatedAtDesc(tenantId, p, status);
        } else {
            rows = status == null
                ? campaignRepository.findByTenantIdOrderByPriorityDescCreatedAtDesc(tenantId)
                : campaignRepository.findByTenantIdAndStatusOrderByPriorityDescCreatedAtDesc(tenantId, status);
        }
        List<CampaignResponse> out = new ArrayList<>();
        for (Campaign c : rows) {
            out.add(toResponse(c, exceedsThreshold(c.getBudgetTotal())));
        }
        return out;
    }

    @Transactional
    public CampaignResponse activate(String tenantId, String campaignUid) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (c.getStatus() == CampaignStatus.EXPIRED) {
            throw new CampaignConflictException("Cannot activate an expired campaign");
        }
        if (c.getStatus() != CampaignStatus.DRAFT && c.getStatus() != CampaignStatus.PAUSED) {
            throw new CampaignConflictException("Cannot activate campaign in status " + c.getStatus());
        }
        Instant now = Instant.now();
        if (c.getValidUntil().isBefore(now)) {
            throw new CampaignBadRequestException("Cannot activate: valid_until is in the past");
        }
        c.setStatus(CampaignStatus.ACTIVE);
        return toResponse(campaignRepository.save(c), exceedsThreshold(c.getBudgetTotal()));
    }

    @Transactional
    public CampaignResponse pause(String tenantId, String campaignUid) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (c.getStatus() != CampaignStatus.ACTIVE) {
            throw new CampaignConflictException("Only ACTIVE campaigns can be paused");
        }
        c.setStatus(CampaignStatus.PAUSED);
        return toResponse(campaignRepository.save(c), exceedsThreshold(c.getBudgetTotal()));
    }

    @Transactional
    public CampaignResponse end(String tenantId, String campaignUid) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (isTerminalStatus(c.getStatus())) {
            throw new CampaignConflictException("Campaign is already terminal: " + c.getStatus());
        }
        c.setStatus(CampaignStatus.ENDED);
        return toResponse(campaignRepository.save(c), exceedsThreshold(c.getBudgetTotal()));
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse stats(String tenantId, String campaignUid) {
        return analyticsService.getCampaignStats(tenantId, campaignUid);
    }

    private Campaign loadCampaign(String tenantId, String campaignUid) {
        return campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));
    }

    private void validateUpsert(String tenantId, String programmeUid, CampaignUpsertRequest req) {
        if (req.getValidUntil() != null && req.getValidFrom() != null && !req.getValidUntil().isAfter(req.getValidFrom())) {
            throw new CampaignBadRequestException("validUntil must be after validFrom");
        }
        programmeValidator.validateTriggerEventType(tenantId, programmeUid, req.getTriggerEventType());
        programmeValidator.validateTierUids(tenantId, programmeUid, req.getTargetSegment());
        programmeValidator.validateOfferConfig(req.getOfferConfig());
        parseStackMode(req.getStackMode());
    }

    private Campaign mapNewEntity(String tenantId, String programmeUid, String campaignUid, CampaignUpsertRequest req, String actorId) {
        Campaign c = new Campaign();
        c.setTenantId(tenantId);
        c.setProgrammeUid(programmeUid);
        c.setCampaignUid(campaignUid);
        c.setStatus(CampaignStatus.DRAFT);
        c.setBudgetConsumed(BigDecimal.ZERO);
        applyUpsert(c, req, actorId);
        return c;
    }

    private void applyUpsert(Campaign c, CampaignUpsertRequest req, String actorId) {
        c.setName(req.getName().trim());
        c.setDescription(req.getDescription());
        c.setCampaignType(
            req.getCampaignType() == null || req.getCampaignType().isBlank()
                ? "STANDARD"
                : req.getCampaignType().trim()
        );
        c.setOccasionTags(toJsonArray(req.getOccasionTags()));
        c.setTargetSegment(toJson(req.getTargetSegment()));
        c.setEligibilityRules(objectMapper.createObjectNode());
        c.setTriggerEventType(TriggerEventTypes.normalize(req.getTriggerEventType()));
        c.setOfferConfig(toJson(req.getOfferConfig()));
        c.setMutualExclGroup(blankToNull(req.getMutualExclGroup()));
        c.setStackMode(parseStackMode(req.getStackMode()));
        c.setBudgetTotal(req.getBudgetTotal().setScale(2, RoundingMode.HALF_UP));
        if (req.getAlertThresholdPct() != null) {
            c.setAlertThresholdPct(req.getAlertThresholdPct().setScale(2, RoundingMode.HALF_UP));
        } else if (c.getAlertThresholdPct() == null) {
            c.setAlertThresholdPct(campaignProperties.getDefaultAlertThresholdPct().setScale(2, RoundingMode.HALF_UP));
        }
        c.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        c.setMaxParticipations(req.getMaxParticipations());
        c.setMaxPerCustomer(req.getMaxPerCustomer());
        c.setGlobalRewardCap(req.getGlobalRewardCap());
        c.setMerchantId(blankToNull(req.getMerchantId()));
        c.setValidFrom(req.getValidFrom());
        c.setValidUntil(req.getValidUntil());
        if (c.getCreatedBy() == null) {
            c.setCreatedBy(actorId);
        }
    }

    private StackMode parseStackMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return StackMode.ADDITIVE;
        }
        try {
            return StackMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new CampaignBadRequestException("Invalid stackMode: " + raw);
        }
    }

    private JsonNode toJson(Object value) {
        if (value == null) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.valueToTree(value);
    }

    private JsonNode toJsonArray(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return objectMapper.createArrayNode();
        }
        ArrayNode arr = objectMapper.createArrayNode();
        for (String t : tags) {
            if (t != null && !t.isBlank()) {
                arr.add(t.trim());
            }
        }
        return arr;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void warnIfBudgetExceedsThreshold(String tenantId, BigDecimal budgetTotal) {
        if (budgetTotal != null && budgetTotal.compareTo(campaignProperties.getApprovalBudgetThreshold()) > 0) {
            log.warn(
                "Campaign budget_total {} exceeds approval threshold {} for tenant {}",
                budgetTotal,
                campaignProperties.getApprovalBudgetThreshold(),
                tenantId
            );
        }
    }

    private boolean exceedsThreshold(BigDecimal budgetTotal) {
        return budgetTotal != null && budgetTotal.compareTo(campaignProperties.getApprovalBudgetThreshold()) > 0;
    }

    private CampaignResponse toResponse(Campaign c, boolean exceedsThreshold) {
        CampaignResponse r = new CampaignResponse();
        r.setTenantId(c.getTenantId());
        r.setProgrammeUid(c.getProgrammeUid());
        r.setCampaignUid(c.getCampaignUid());
        r.setName(c.getName());
        r.setDescription(c.getDescription());
        r.setCampaignType(c.getCampaignType());
        r.setOccasionTags(c.getOccasionTags());
        r.setStatus(c.getStatus());
        r.setTargetSegment(c.getTargetSegment());
        r.setEligibilityRules(c.getEligibilityRules());
        r.setTriggerEventType(c.getTriggerEventType());
        r.setOfferConfig(c.getOfferConfig());
        r.setMutualExclGroup(c.getMutualExclGroup());
        r.setStackMode(c.getStackMode());
        r.setBudgetTotal(c.getBudgetTotal());
        r.setBudgetConsumed(c.getBudgetConsumed());
        r.setBudgetConsumedPct(consumedPct(c.getBudgetConsumed(), c.getBudgetTotal()));
        r.setBudgetRemaining(c.getBudgetTotal().subtract(c.getBudgetConsumed()).max(BigDecimal.ZERO));
        r.setAlertThresholdPct(c.getAlertThresholdPct());
        r.setPriority(c.getPriority());
        r.setMaxParticipations(c.getMaxParticipations());
        r.setMaxPerCustomer(c.getMaxPerCustomer());
        r.setGlobalRewardCap(c.getGlobalRewardCap());
        r.setMerchantId(c.getMerchantId());
        r.setValidFrom(c.getValidFrom());
        r.setValidUntil(c.getValidUntil());
        r.setCreatedBy(c.getCreatedBy());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        r.setBudgetExceedsApprovalThreshold(exceedsThreshold);
        return r;
    }

    private static String defaultProgrammeUid(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }

    private static boolean isTerminalStatus(CampaignStatus status) {
        return status == CampaignStatus.ENDED
            || status == CampaignStatus.EXHAUSTED
            || status == CampaignStatus.EXPIRED;
    }

    private static BigDecimal consumedPct(BigDecimal consumed, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal c = consumed == null ? BigDecimal.ZERO : consumed;
        return c.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }
}
