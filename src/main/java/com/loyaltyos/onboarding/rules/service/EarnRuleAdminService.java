package com.loyaltyos.onboarding.rules.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.rules.dto.EarnRuleResponse;
import com.loyaltyos.onboarding.rules.dto.EarnRuleDetailResponse;
import com.loyaltyos.onboarding.rules.dto.RuleChangeLogResponse;
import com.loyaltyos.onboarding.rules.dto.RuleStatusPatchRequest;
import com.loyaltyos.onboarding.rules.dto.RuleUpsertRequest;
import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.entity.RuleAction;
import com.loyaltyos.onboarding.rules.entity.RuleChangeLog;
import com.loyaltyos.onboarding.rules.entity.RuleCondition;
import com.loyaltyos.onboarding.rules.enums.ActionType;
import com.loyaltyos.onboarding.rules.enums.ExecutionMode;
import com.loyaltyos.onboarding.rules.enums.RuleChangeType;
import com.loyaltyos.onboarding.rules.enums.RuleStatus;
import com.loyaltyos.onboarding.rules.enums.RuleType;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.util.TriggerEventTypes;
import com.loyaltyos.onboarding.rules.evaluation.ConditionParseException;
import com.loyaltyos.onboarding.rules.evaluation.ConditionTreeParser;
import com.loyaltyos.onboarding.rules.exception.RuleEngineBadRequestException;
import com.loyaltyos.onboarding.rules.repository.EarnRuleRepository;
import com.loyaltyos.onboarding.rules.repository.RuleChangeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class EarnRuleAdminService {

    private final EarnRuleRepository earnRuleRepository;
    private final RuleChangeLogRepository ruleChangeLogRepository;
    private final ConditionTreeParser conditionTreeParser;
    private final ProgrammeRuleContextLoader programmeRuleContextLoader;
    private final RuleCacheService ruleCacheService;
    private final CampaignRepository campaignRepository;
    private final ObjectMapper objectMapper;

    public EarnRuleAdminService(
        EarnRuleRepository earnRuleRepository,
        RuleChangeLogRepository ruleChangeLogRepository,
        ConditionTreeParser conditionTreeParser,
        ProgrammeRuleContextLoader programmeRuleContextLoader,
        RuleCacheService ruleCacheService,
        CampaignRepository campaignRepository,
        ObjectMapper objectMapper
    ) {
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
        this.ruleChangeLogRepository = Objects.requireNonNull(ruleChangeLogRepository, "ruleChangeLogRepository");
        this.conditionTreeParser = Objects.requireNonNull(conditionTreeParser, "conditionTreeParser");
        this.programmeRuleContextLoader = Objects.requireNonNull(programmeRuleContextLoader, "programmeRuleContextLoader");
        this.ruleCacheService = Objects.requireNonNull(ruleCacheService, "ruleCacheService");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    private record RuleLinkage(RuleType ruleType, String campaignUid, String programmeUid) {}

    @Transactional
    public EarnRuleResponse createRule(String tenantId, String programmeUid, RuleUpsertRequest req, String actorId) {
        RuleLinkage linkage = resolveRuleLinkage(tenantId, programmeUid, req);
        programmeUid = linkage.programmeUid();
        ProgrammeEvaluationContext ctx = programmeRuleContextLoader.load(tenantId, programmeUid, null);
        try {
            conditionTreeParser.parseConditionTree(req.getConditionTree(), ctx.getAllowedEventPropertyNames());
        } catch (ConditionParseException e) {
            throw new RuleEngineBadRequestException(e.getMessage());
        }
        validateEffectiveWindow(req.getEffectiveAt(), req.getEndAt());

        String ruleUid = (req.getRuleUid() != null && !req.getRuleUid().isBlank())
            ? req.getRuleUid()
            : UUID.randomUUID().toString();

        EarnRule rule = EarnRule.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .ruleType(linkage.ruleType())
            .campaignUid(linkage.campaignUid())
            .ruleUid(ruleUid)
            .name(req.getName().trim())
            .description(req.getDescription())
            .priority(req.getPriority())
            .status(req.getStatus())
            .triggerEventType(req.getTriggerEventType().trim())
            .executionMode(parseExecutionMode(req.getExecutionMode()))
            .effectiveAt(req.getEffectiveAt())
            .endAt(req.getEndAt())
            .build();

        RuleCondition cond = RuleCondition.builder()
            .tenantId(tenantId)
            .rule(rule)
            .conditionTree(req.getConditionTree())
            .build();
        rule.setCondition(cond);

        for (RuleUpsertRequest.RuleActionUpsertItem a : req.getActions()) {
            String actionUid = (a.getActionUid() != null && !a.getActionUid().isBlank())
                ? a.getActionUid()
                : UUID.randomUUID().toString();
            ActionType at;
            try {
                at = ActionType.valueOf(a.getActionType());
            } catch (Exception ex) {
                throw new RuleEngineBadRequestException("Invalid actionType: " + a.getActionType());
            }
            RuleAction ra = RuleAction.builder()
                .tenantId(tenantId)
                .rule(rule)
                .actionUid(actionUid)
                .actionType(at)
                .formula(a.getFormula())
                .config(a.getConfig())
                .build();
            rule.getActions().add(ra);
        }

        EarnRule saved = earnRuleRepository.save(rule);

        ruleChangeLogRepository.save(RuleChangeLog.builder()
            .rule(saved)
            .tenantId(tenantId)
            .changeType(RuleChangeType.CREATED)
            .changedBy(actorId)
            .beforeState(null)
            .afterState(toJson(summary(saved)))
            .build());

        ruleCacheService.invalidateProgramme(tenantId, programmeUid);
        return toResponse(saved);
    }

    @Transactional
    public EarnRuleResponse updateRule(String tenantId, String programmeUid, String ruleUid, RuleUpsertRequest req, String actorId) {
        EarnRule rule = earnRuleRepository.loadForAdminEdit(tenantId, programmeUid, ruleUid)
            .orElseThrow(() -> new RuleEngineBadRequestException("Rule not found: " + ruleUid));
        if (rule.getStatus() == RuleStatus.ARCHIVED) {
            throw new RuleEngineBadRequestException("Cannot update an archived rule; unarchive via status first");
        }

        resolveRuleLinkage(tenantId, programmeUid, req);

        ProgrammeEvaluationContext ctx = programmeRuleContextLoader.load(tenantId, programmeUid, null);
        try {
            conditionTreeParser.parseConditionTree(req.getConditionTree(), ctx.getAllowedEventPropertyNames());
        } catch (ConditionParseException e) {
            throw new RuleEngineBadRequestException(e.getMessage());
        }
        validateEffectiveWindow(req.getEffectiveAt(), req.getEndAt());

        Map<String, Object> beforeState = fullSnapshot(rule);

        rule.setName(req.getName().trim());
        rule.setDescription(req.getDescription());
        rule.setPriority(req.getPriority());
        rule.setStatus(req.getStatus());
        rule.setTriggerEventType(req.getTriggerEventType().trim());
        rule.setExecutionMode(parseExecutionMode(req.getExecutionMode()));
        rule.setEffectiveAt(req.getEffectiveAt());
        rule.setEndAt(req.getEndAt());

        RuleCondition cond = rule.getCondition();
        if (cond == null) {
            cond = RuleCondition.builder()
                .tenantId(tenantId)
                .rule(rule)
                .conditionTree(req.getConditionTree())
                .build();
            rule.setCondition(cond);
        } else {
            cond.setConditionTree(req.getConditionTree());
        }

        rule.getActions().clear();
        for (RuleUpsertRequest.RuleActionUpsertItem a : req.getActions()) {
            String actionUid = (a.getActionUid() != null && !a.getActionUid().isBlank())
                ? a.getActionUid()
                : UUID.randomUUID().toString();
            ActionType at;
            try {
                at = ActionType.valueOf(a.getActionType());
            } catch (Exception ex) {
                throw new RuleEngineBadRequestException("Invalid actionType: " + a.getActionType());
            }
            RuleAction ra = RuleAction.builder()
                .tenantId(tenantId)
                .rule(rule)
                .actionUid(actionUid)
                .actionType(at)
                .formula(a.getFormula())
                .config(a.getConfig())
                .build();
            rule.getActions().add(ra);
        }

        EarnRule saved = earnRuleRepository.save(rule);

        ruleChangeLogRepository.save(RuleChangeLog.builder()
            .rule(saved)
            .tenantId(tenantId)
            .changeType(RuleChangeType.UPDATED)
            .changedBy(actorId)
            .beforeState(toJson(beforeState))
            .afterState(toJson(fullSnapshot(saved)))
            .build());

        ruleCacheService.invalidateProgramme(tenantId, programmeUid);
        return toResponse(saved);
    }

    @Transactional
    public void deleteRule(String tenantId, String programmeUid, String ruleUid, String actorId) {
        EarnRule rule = earnRuleRepository.loadForAdminEdit(tenantId, programmeUid, ruleUid)
            .orElseThrow(() -> new RuleEngineBadRequestException("Rule not found: " + ruleUid));
        if (rule.getStatus() == RuleStatus.ARCHIVED) {
            throw new RuleEngineBadRequestException("Rule already archived: " + ruleUid);
        }

        Map<String, Object> beforeState = fullSnapshot(rule);
        rule.setStatus(RuleStatus.ARCHIVED);
        rule.setArchivedAt(Instant.now());
        EarnRule saved = earnRuleRepository.save(rule);

        ruleChangeLogRepository.save(RuleChangeLog.builder()
            .rule(saved)
            .tenantId(tenantId)
            .changeType(RuleChangeType.DELETED)
            .changedBy(actorId)
            .beforeState(toJson(beforeState))
            .afterState(toJson(fullSnapshot(saved)))
            .build());

        ruleCacheService.invalidateProgramme(tenantId, programmeUid);
    }

    @Transactional
    public EarnRuleResponse updateStatus(String tenantId, String programmeUid, String ruleUid, RuleStatusPatchRequest req, String actorId) {
        EarnRule rule = earnRuleRepository.findByTenantIdAndProgrammeUidAndRuleUid(tenantId, programmeUid, ruleUid)
            .orElseThrow(() -> new RuleEngineBadRequestException("Rule not found: " + ruleUid));
        RuleStatus before = rule.getStatus();
        RuleStatus next = req.getStatus();
        rule.setStatus(next);
        if (next == RuleStatus.ARCHIVED && rule.getArchivedAt() == null) {
            rule.setArchivedAt(Instant.now());
        } else if (before == RuleStatus.ARCHIVED && next != RuleStatus.ARCHIVED) {
            rule.setArchivedAt(null);
        }
        EarnRule saved = earnRuleRepository.save(rule);

        ruleChangeLogRepository.save(RuleChangeLog.builder()
            .rule(saved)
            .tenantId(tenantId)
            .changeType(RuleChangeType.STATUS_CHANGED)
            .changedBy(actorId)
            .beforeState(toJson(Map.of("status", String.valueOf(before))))
            .afterState(toJson(Map.of("status", String.valueOf(saved.getStatus()))))
            .build());

        ruleCacheService.invalidateProgramme(tenantId, programmeUid);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EarnRuleResponse> listRules(String tenantId, String programmeUid, String ruleTypeFilter) {
        RuleType filter = parseRuleTypeFilter(ruleTypeFilter);
        return earnRuleRepository.findByTenantIdAndProgrammeUidOrderByPriorityDesc(tenantId, programmeUid)
            .stream()
            .filter(r -> filter == null || r.getRuleType() == filter)
            .map(EarnRuleAdminService::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public EarnRuleDetailResponse getRule(String tenantId, String programmeUid, String ruleUid) {
        EarnRule rule = earnRuleRepository.loadForAdminEdit(tenantId, programmeUid, ruleUid)
            .orElseThrow(() -> new RuleEngineBadRequestException("Rule not found: " + ruleUid));
        return toDetailResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<RuleChangeLogResponse> getChangeHistory(String tenantId, String programmeUid, String ruleUid) {
        EarnRule rule = earnRuleRepository.findByTenantIdAndProgrammeUidAndRuleUid(tenantId, programmeUid, ruleUid)
            .orElseThrow(() -> new RuleEngineBadRequestException("Rule not found: " + ruleUid));
        return ruleChangeLogRepository.findByTenantIdAndRule_IdOrderByChangedAtDesc(tenantId, rule.getId())
            .stream()
            .map(l -> RuleChangeLogResponse.builder()
                .id(l.getId())
                .ruleUid(ruleUid)
                .changeType(l.getChangeType())
                .changedBy(l.getChangedBy())
                .beforeState(l.getBeforeState())
                .afterState(l.getAfterState())
                .changedAt(l.getChangedAt())
                .build())
            .toList();
    }

    private static ExecutionMode parseExecutionMode(String s) {
        try {
            return ExecutionMode.valueOf(s.trim());
        } catch (Exception e) {
            throw new RuleEngineBadRequestException("Invalid executionMode: " + s);
        }
    }

    private RuleLinkage resolveRuleLinkage(String tenantId, String programmeUid, RuleUpsertRequest req) {
        RuleType type = parseRuleType(req.getRuleType());
        if (type == RuleType.CAMPAIGN) {
            String campaignUid = req.getCampaignUid();
            if (campaignUid == null || campaignUid.isBlank()) {
                throw new RuleEngineBadRequestException("campaignUid is required for CAMPAIGN rules");
            }
            Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid.trim())
                .orElseThrow(() -> new RuleEngineBadRequestException("Campaign not found: " + campaignUid));
            String prog = campaign.getProgrammeUid() != null && !campaign.getProgrammeUid().isBlank()
                ? campaign.getProgrammeUid()
                : "default";
            String campaignTrigger = campaign.getTriggerEventType();
            try {
                String normalized = TriggerEventTypes.resolveSingleForCampaignRule(
                    campaignTrigger,
                    req.getTriggerEventType()
                );
                req.setTriggerEventType(normalized);
            } catch (IllegalArgumentException e) {
                throw new RuleEngineBadRequestException(e.getMessage());
            }
            return new RuleLinkage(RuleType.CAMPAIGN, campaign.getCampaignUid(), prog);
        }
        String prog = programmeUid != null && !programmeUid.isBlank() ? programmeUid.trim() : "default";
        if (req.getProgrammeUid() != null && !req.getProgrammeUid().isBlank()) {
            prog = req.getProgrammeUid().trim();
        }
        return new RuleLinkage(RuleType.PROGRAMME, null, prog);
    }

    private static RuleType parseRuleType(String raw) {
        if (raw == null || raw.isBlank()) {
            return RuleType.PROGRAMME;
        }
        try {
            return RuleType.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuleEngineBadRequestException("Invalid ruleType: " + raw);
        }
    }

    private static RuleType parseRuleTypeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseRuleType(raw);
    }

    private static Map<String, Object> summary(EarnRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruleUid", r.getRuleUid());
        m.put("name", r.getName());
        m.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        m.put("ruleType", r.getRuleType() != null ? r.getRuleType().name() : null);
        m.put("campaignUid", r.getCampaignUid());
        m.put("triggerEventType", r.getTriggerEventType());
        return m;
    }

    private Map<String, Object> fullSnapshot(EarnRule r) {
        Map<String, Object> m = new LinkedHashMap<>(summary(r));
        m.put("description", r.getDescription());
        m.put("priority", r.getPriority());
        m.put("executionMode", r.getExecutionMode() != null ? r.getExecutionMode().name() : null);
        m.put("effectiveAt", r.getEffectiveAt() != null ? r.getEffectiveAt().toString() : null);
        m.put("endAt", r.getEndAt() != null ? r.getEndAt().toString() : null);
        m.put("activatedAt", r.getActivatedAt() != null ? r.getActivatedAt().toString() : null);
        m.put("archivedAt", r.getArchivedAt() != null ? r.getArchivedAt().toString() : null);
        if (r.getCondition() != null && r.getCondition().getConditionTree() != null) {
            m.put("conditionTree", objectMapper.convertValue(
                r.getCondition().getConditionTree(), new TypeReference<Map<String, Object>>() {}));
        } else {
            m.put("conditionTree", null);
        }
        List<Map<String, Object>> actions = new ArrayList<>();
        for (RuleAction a : r.getActions()) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("actionUid", a.getActionUid());
            am.put("actionType", a.getActionType() != null ? a.getActionType().name() : null);
            am.put("formula", a.getFormula());
            if (a.getConfig() != null) {
                am.put("config", objectMapper.convertValue(a.getConfig(), new TypeReference<Map<String, Object>>() {}));
            } else {
                am.put("config", null);
            }
            actions.add(am);
        }
        m.put("actions", actions);
        return m;
    }

    private static void validateEffectiveWindow(Instant effectiveAt, Instant endAt) {
        if (effectiveAt != null && endAt != null && !endAt.isAfter(effectiveAt)) {
            throw new RuleEngineBadRequestException("endAt must be after effectiveAt when both are set");
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static EarnRuleResponse toResponse(EarnRule r) {
        return EarnRuleResponse.builder()
            .id(r.getId())
            .tenantId(r.getTenantId())
            .programmeUid(r.getProgrammeUid())
            .ruleType(r.getRuleType() != null ? r.getRuleType().name() : RuleType.PROGRAMME.name())
            .campaignUid(r.getCampaignUid())
            .ruleUid(r.getRuleUid())
            .name(r.getName())
            .status(r.getStatus())
            .triggerEventType(r.getTriggerEventType())
            .executionMode(r.getExecutionMode() != null ? r.getExecutionMode().name() : null)
            .build();
    }

    private static EarnRuleDetailResponse toDetailResponse(EarnRule r) {
        List<EarnRuleDetailResponse.ActionItem> actions = r.getActions().stream()
            .map(a -> EarnRuleDetailResponse.ActionItem.builder()
                .id(a.getId())
                .actionUid(a.getActionUid())
                .actionType(a.getActionType() != null ? a.getActionType().name() : null)
                .formula(a.getFormula())
                .config(a.getConfig())
                .build())
            .toList();
        return EarnRuleDetailResponse.builder()
            .id(r.getId())
            .tenantId(r.getTenantId())
            .programmeUid(r.getProgrammeUid())
            .ruleType(r.getRuleType() != null ? r.getRuleType().name() : RuleType.PROGRAMME.name())
            .campaignUid(r.getCampaignUid())
            .ruleUid(r.getRuleUid())
            .name(r.getName())
            .description(r.getDescription())
            .priority(r.getPriority())
            .status(r.getStatus())
            .triggerEventType(r.getTriggerEventType())
            .executionMode(r.getExecutionMode() != null ? r.getExecutionMode().name() : null)
            .effectiveAt(r.getEffectiveAt())
            .endAt(r.getEndAt())
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .activatedAt(r.getActivatedAt())
            .archivedAt(r.getArchivedAt())
            .conditionTree(r.getCondition() != null ? r.getCondition().getConditionTree() : null)
            .actions(actions)
            .build();
    }
}
