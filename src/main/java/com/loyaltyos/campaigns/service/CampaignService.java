package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.campaigns.dto.CampaignEventSchemaUpsertRequest;
import com.loyaltyos.onboarding.dto.EventDefinitionRequest;
import com.loyaltyos.onboarding.dto.EventSchemaSettingsPatchRequest;
import com.loyaltyos.onboarding.service.EventSchemaJsonSupport;
import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignSetupStatusResponse;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignExecutionMode;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.enums.StackMode;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.rules.entity.EarnRule;
import com.loyaltyos.rules.enums.RuleStatus;
import com.loyaltyos.rules.enums.RuleType;
import com.loyaltyos.rules.repository.EarnRuleRepository;
import com.loyaltyos.campaigns.util.TriggerEventTypes;
import com.loyaltyos.onboarding.service.ProgrammeService;
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
    private final ProgrammeService programmeService;
    private final EarnRuleRepository earnRuleRepository;
    private final CampaignRuleSandboxService campaignRuleSandboxService;

    public CampaignService(
        CampaignRepository campaignRepository,
        CampaignAnalyticsService analyticsService,
        CampaignProgrammeValidator programmeValidator,
        CampaignProperties campaignProperties,
        ObjectMapper objectMapper,
        ProgrammeService programmeService,
        EarnRuleRepository earnRuleRepository,
        CampaignRuleSandboxService campaignRuleSandboxService
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
        this.programmeValidator = Objects.requireNonNull(programmeValidator, "programmeValidator");
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
        this.campaignRuleSandboxService = Objects.requireNonNull(campaignRuleSandboxService, "campaignRuleSandboxService");
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
        Campaign saved = campaignRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse update(String tenantId, String campaignUid, CampaignUpsertRequest req, String actorId) {
        assertCampaignsEnabled();
        Campaign existing = loadCampaign(tenantId, campaignUid);
        assertCampaignEditableForUpdate(existing);

        String programmeUid = programmeValidator.requireProgrammeUid(req.getProgrammeUid());
        if (!programmeUid.equals(existing.getProgrammeUid())) {
            throw new CampaignBadRequestException("programmeUid cannot be changed on update");
        }
        programmeValidator.assertProgrammeExists(tenantId, programmeUid);
        validateUpsert(tenantId, programmeUid, req);

        applyUpsert(existing, req, actorId);
        Campaign saved = campaignRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(String tenantId, String campaignUid) {
        Campaign c = loadCampaign(tenantId, campaignUid);
        return toResponse(c);
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
        rows = rows.stream()
            .filter(c -> !programmeService.isProgrammeArchived(tenantId, c.getProgrammeUid()))
            .toList();
        List<CampaignResponse> out = new ArrayList<>();
        for (Campaign c : rows) {
            out.add(toResponse(c));
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
        if (c.getCustomerScope() == CustomerScope.TARGETED) {
            int count = c.getCustomerCount() != null ? c.getCustomerCount() : 0;
            if (count <= 0) {
                throw new CampaignBadRequestException(
                    "Upload a customer list before activating a targeted campaign"
                );
            }
        }
        assertRuleGatedActivationPreconditions(tenantId, c);
        c.setStatus(CampaignStatus.ACTIVE);
        return toResponse(campaignRepository.save(c));
    }

    @Transactional(readOnly = true)
    public CampaignSetupStatusResponse getSetupStatus(String tenantId, String campaignUid) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        List<EarnRule> campaignRules = earnRuleRepository.findByTenantIdAndCampaignUidAndRuleTypeOrderByPriorityDesc(
            tenantId, campaignUid, RuleType.CAMPAIGN
        );
        EarnRule primaryRule = campaignRules.isEmpty() ? null : campaignRules.getFirst();
        boolean ruleActive = primaryRule != null && primaryRule.getStatus() == RuleStatus.ACTIVE;
        boolean sandboxPassed = primaryRule != null
            && campaignRuleSandboxService.hasSandboxPass(tenantId, primaryRule.getRuleUid());

        CampaignSetupStatusResponse status = new CampaignSetupStatusResponse();
        status.setCampaignUid(campaignUid);
        status.setCampaignStatus(c.getStatus());
        status.setExecutionMode(c.getExecutionMode() != null ? c.getExecutionMode() : CampaignExecutionMode.RULE_GATED);
        status.setCampaignSaved(c.getStatus() == CampaignStatus.DRAFT || c.getStatus() == CampaignStatus.PAUSED);
        status.setCampaignRuleCreated(primaryRule != null);
        status.setCampaignRuleUid(primaryRule != null ? primaryRule.getRuleUid() : null);
        status.setCampaignRuleActive(ruleActive);
        status.setSandboxPassed(sandboxPassed);
        if (sandboxPassed && primaryRule != null) {
            status.setSandboxPassedAt(
                campaignRuleSandboxService.getSandboxStatus(tenantId, primaryRule.getRuleUid()).getPassedAt()
            );
        }

        String blockReason = null;
        boolean canActivate = c.getStatus() == CampaignStatus.DRAFT || c.getStatus() == CampaignStatus.PAUSED;
        if (canActivate && status.getExecutionMode() == CampaignExecutionMode.RULE_GATED && campaignProperties.isRuleGatedOnly()) {
            if (primaryRule == null) {
                canActivate = false;
                blockReason = "Create a CAMPAIGN earn rule for this campaign";
            } else if (!ruleActive) {
                canActivate = false;
                blockReason = "Activate the CAMPAIGN earn rule after sandbox passes";
            } else if (!sandboxPassed) {
                canActivate = false;
                blockReason = "Pass sandbox test for the CAMPAIGN earn rule";
            }
        }
        status.setCanActivateCampaign(canActivate);
        status.setActivateBlockReason(blockReason);
        return status;
    }

    private void assertRuleGatedActivationPreconditions(String tenantId, Campaign c) {
        if (c.getExecutionMode() != CampaignExecutionMode.RULE_GATED || !campaignProperties.isRuleGatedOnly()) {
            return;
        }
        List<EarnRule> activeRules = earnRuleRepository.findByTenantIdAndCampaignUidAndRuleTypeAndStatus(
            tenantId, c.getCampaignUid(), RuleType.CAMPAIGN, RuleStatus.ACTIVE
        );
        if (activeRules.isEmpty()) {
            List<EarnRule> anyRules = earnRuleRepository.findByTenantIdAndCampaignUidAndRuleTypeOrderByPriorityDesc(
                tenantId, c.getCampaignUid(), RuleType.CAMPAIGN
            );
            if (anyRules.isEmpty()) {
                throw new CampaignConflictException(
                    "CAMPAIGN_RULE_REQUIRED",
                    "Create and activate a CAMPAIGN earn rule before activating this campaign"
                );
            }
            throw new CampaignConflictException(
                "CAMPAIGN_RULE_NOT_ACTIVE",
                "Activate the CAMPAIGN earn rule before activating this campaign"
            );
        }
        EarnRule rule = activeRules.getFirst();
        if (!campaignRuleSandboxService.hasSandboxPass(tenantId, rule.getRuleUid())) {
            throw new CampaignConflictException(
                "SANDBOX_REQUIRED",
                "Pass sandbox test for the CAMPAIGN earn rule before activating this campaign"
            );
        }
    }

    @Transactional
    public CampaignResponse pause(String tenantId, String campaignUid) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (c.getStatus() != CampaignStatus.ACTIVE) {
            throw new CampaignConflictException("Only ACTIVE campaigns can be paused");
        }
        c.setStatus(CampaignStatus.PAUSED);
        return toResponse(campaignRepository.save(c));
    }

    @Transactional
    public CampaignResponse end(String tenantId, String campaignUid) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (isTerminalStatus(c.getStatus())) {
            throw new CampaignConflictException("Campaign is already terminal: " + c.getStatus());
        }
        c.setStatus(CampaignStatus.ENDED);
        return toResponse(campaignRepository.save(c));
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse stats(String tenantId, String campaignUid) {
        return analyticsService.getCampaignStats(tenantId, campaignUid);
    }

    @Transactional(readOnly = true)
    public JsonNode getEventSchema(String tenantId, String campaignUid) {
        Campaign c = loadCampaign(tenantId, campaignUid);
        return c.getEventSchema() != null ? c.getEventSchema() : objectMapper.createObjectNode();
    }

    @Transactional
    public CampaignResponse upsertEventSchema(String tenantId, String campaignUid, CampaignEventSchemaUpsertRequest req) {
        assertCampaignsEnabled();
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (isTerminalStatus(c.getStatus())) {
            throw new CampaignConflictException("Cannot update event schema for campaign in status " + c.getStatus());
        }
        JsonNode schema = req.getEventSchema();
        if (schema == null || schema.isNull()) {
            throw new CampaignBadRequestException("eventSchema is required");
        }
        String validationError = EventSchemaJsonSupport.validateEventSchemaDocument(schema);
        if (validationError != null) {
            throw new CampaignBadRequestException(validationError);
        }
        ObjectNode stored = schema.deepCopy();
        EventSchemaJsonSupport.ensureStandardFieldsUnion(stored);
        c.setEventSchema(stored);
        String triggerTypes = EventSchemaJsonSupport.triggerTypesFromEventSchema(stored);
        if (triggerTypes.isBlank()) {
            throw new CampaignBadRequestException("At least one event type is required in eventSchema");
        }
        c.setTriggerEventType(triggerTypes);
        programmeValidator.validateTriggerEventType(tenantId, c.getProgrammeUid(), triggerTypes);
        Campaign saved = campaignRepository.save(c);
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse patchEventDefinition(
        String tenantId,
        String campaignUid,
        String pathEventType,
        EventDefinitionRequest body
    ) {
        assertCampaignsEnabled();
        Campaign c = loadEditableCampaign(tenantId, campaignUid);
        ObjectNode schema = mutableEventSchema(c);
        ObjectNode definition = EventSchemaJsonSupport.toEventDefinitionNode(body);
        try {
            EventSchemaJsonSupport.replaceEventDefinition(schema, pathEventType, definition);
        } catch (IllegalArgumentException e) {
            throw new CampaignBadRequestException(e.getMessage());
        }
        return persistEventSchemaDocument(c, schema);
    }

    @Transactional
    public CampaignResponse addEventDefinition(String tenantId, String campaignUid, EventDefinitionRequest body) {
        assertCampaignsEnabled();
        Campaign c = loadEditableCampaign(tenantId, campaignUid);
        ObjectNode schema = mutableEventSchema(c);
        ObjectNode definition = EventSchemaJsonSupport.toEventDefinitionNode(body);
        try {
            EventSchemaJsonSupport.addEventDefinition(schema, definition);
        } catch (IllegalArgumentException e) {
            throw new CampaignBadRequestException(e.getMessage());
        }
        return persistEventSchemaDocument(c, schema);
    }

    @Transactional
    public CampaignResponse removeEventDefinition(String tenantId, String campaignUid, String pathEventType) {
        assertCampaignsEnabled();
        Campaign c = loadEditableCampaign(tenantId, campaignUid);
        ObjectNode schema = mutableEventSchema(c);
        try {
            EventSchemaJsonSupport.removeEventDefinition(schema, pathEventType);
        } catch (IllegalArgumentException e) {
            throw new CampaignBadRequestException(e.getMessage());
        }
        return persistEventSchemaDocument(c, schema);
    }

    @Transactional
    public CampaignResponse patchEventSchemaSettings(
        String tenantId,
        String campaignUid,
        EventSchemaSettingsPatchRequest body
    ) {
        assertCampaignsEnabled();
        Campaign c = loadEditableCampaign(tenantId, campaignUid);
        ObjectNode schema = mutableEventSchema(c);
        try {
            EventSchemaJsonSupport.applySettingsPatch(schema, body);
        } catch (IllegalArgumentException e) {
            throw new CampaignBadRequestException(e.getMessage());
        }
        return persistEventSchemaDocument(c, schema);
    }

    private Campaign loadEditableCampaign(String tenantId, String campaignUid) {
        Campaign c = loadCampaign(tenantId, campaignUid);
        if (isTerminalStatus(c.getStatus())) {
            throw new CampaignConflictException("Cannot update event schema for campaign in status " + c.getStatus());
        }
        return c;
    }

    private ObjectNode mutableEventSchema(Campaign c) {
        JsonNode existing = c.getEventSchema();
        if (existing != null && existing.isObject()) {
            return existing.deepCopy();
        }
        return objectMapper.createObjectNode();
    }

    private CampaignResponse persistEventSchemaDocument(Campaign c, ObjectNode schema) {
        c.setEventSchema(schema);
        String triggerTypes = EventSchemaJsonSupport.triggerTypesFromEventSchema(schema);
        if (triggerTypes.isBlank()) {
            throw new CampaignBadRequestException("At least one event type is required in eventSchema");
        }
        c.setTriggerEventType(triggerTypes);
        programmeValidator.validateTriggerEventType(c.getTenantId(), c.getProgrammeUid(), triggerTypes);
        Campaign saved = campaignRepository.save(c);
        return toResponse(saved, exceedsThreshold(saved.getBudgetTotal()));
    }

    private Campaign loadCampaign(String tenantId, String campaignUid) {
        return campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));
    }

    private void validateUpsert(String tenantId, String programmeUid, CampaignUpsertRequest req) {
        if (req.getValidUntil() != null && req.getValidFrom() != null && !req.getValidUntil().isAfter(req.getValidFrom())) {
            throw new CampaignBadRequestException("validUntil must be after validFrom");
        }
        programmeValidator.validateTriggerEventTypeIfPresent(tenantId, programmeUid, req.getTriggerEventType());
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
        c.setExecutionMode(CampaignExecutionMode.RULE_GATED);
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
        if (req.getCustomerScope() != null && !req.getCustomerScope().isBlank()) {
            c.setCustomerScope(parseCustomerScope(req.getCustomerScope()));
        } else if (c.getCustomerScope() == null) {
            c.setCustomerScope(CustomerScope.ALL);
        }
        if (c.getCreatedBy() == null) {
            c.setCreatedBy(actorId);
        }
    }

    private CustomerScope parseCustomerScope(String raw) {
        if (raw == null || raw.isBlank()) {
            return CustomerScope.ALL;
        }
        try {
            return CustomerScope.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new CampaignBadRequestException("Invalid customerScope: " + raw);
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

    private CampaignResponse toResponse(Campaign c) {
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
        r.setEventSchema(c.getEventSchema());
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
        r.setCustomerScope(c.getCustomerScope() != null ? c.getCustomerScope() : CustomerScope.ALL);
        r.setCustomerCount(c.getCustomerCount() != null ? c.getCustomerCount() : 0);
        r.setExecutionMode(c.getExecutionMode() != null ? c.getExecutionMode() : CampaignExecutionMode.RULE_GATED);
        return r;
    }

    private static String defaultProgrammeUid(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }

    private static void assertCampaignEditableForUpdate(Campaign campaign) {
        CampaignStatus status = campaign.getStatus();
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.PAUSED) {
            throw new CampaignConflictException(
                "Only draft or paused campaigns can be updated (current status: " + status + ")"
            );
        }
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
