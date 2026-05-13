package com.loyaltyos.onboarding.rules.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.onboarding.rules.cache.CachedActionSnapshot;
import com.loyaltyos.onboarding.rules.cache.CachedRuleSnapshot;
import com.loyaltyos.onboarding.rules.config.RulesProperties;
import com.loyaltyos.onboarding.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.onboarding.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.enums.ActionType;
import com.loyaltyos.onboarding.rules.enums.ExecutionMode;
import com.loyaltyos.onboarding.rules.enums.RuleStatus;
import com.loyaltyos.onboarding.rules.evaluation.ConditionParseException;
import com.loyaltyos.onboarding.rules.evaluation.ConditionTreeParser;
import com.loyaltyos.onboarding.rules.evaluation.SpelEvaluationService;
import com.loyaltyos.onboarding.rules.repository.EarnRuleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RuleEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluationService.class);

    private final EarnRuleRepository earnRuleRepository;
    private final ProgrammeRuleContextLoader programmeRuleContextLoader;
    private final ConditionTreeParser conditionTreeParser;
    private final SpelEvaluationService spelEvaluationService;
    private final RuleCacheService ruleCacheService;
    private final RulesProperties rulesProperties;
    private final ObjectMapper objectMapper;
    private final RuleEvaluationAuditWriter auditWriter;
    private final RuleEarningCapService ruleEarningCapService;
    private final ObjectProvider<MeterRegistry> meterRegistry;

    public RuleEvaluationService(
        EarnRuleRepository earnRuleRepository,
        ProgrammeRuleContextLoader programmeRuleContextLoader,
        ConditionTreeParser conditionTreeParser,
        SpelEvaluationService spelEvaluationService,
        RuleCacheService ruleCacheService,
        RulesProperties rulesProperties,
        ObjectMapper objectMapper,
        RuleEvaluationAuditWriter auditWriter,
        RuleEarningCapService ruleEarningCapService,
        ObjectProvider<MeterRegistry> meterRegistry
    ) {
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
        this.programmeRuleContextLoader = Objects.requireNonNull(programmeRuleContextLoader, "programmeRuleContextLoader");
        this.conditionTreeParser = Objects.requireNonNull(conditionTreeParser, "conditionTreeParser");
        this.spelEvaluationService = Objects.requireNonNull(spelEvaluationService, "spelEvaluationService");
        this.ruleCacheService = Objects.requireNonNull(ruleCacheService, "ruleCacheService");
        this.rulesProperties = Objects.requireNonNull(rulesProperties, "rulesProperties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.auditWriter = Objects.requireNonNull(auditWriter, "auditWriter");
        this.ruleEarningCapService = Objects.requireNonNull(ruleEarningCapService, "ruleEarningCapService");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    @Transactional(readOnly = true)
    public RuleEvaluationResponse evaluate(String tenantId, RuleEvaluateRequest request) {
        MeterRegistry reg = meterRegistry.getIfAvailable();
        long t0 = System.nanoTime();
        try {
            return evaluateInternal(tenantId, request);
        } finally {
            if (reg != null) {
                reg.timer("loyalty.rules.evaluation").record(System.nanoTime() - t0, TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * Sandbox/admin helper: evaluate a specific rule by ruleUid regardless of status (e.g., DRAFT).
     * This lets the portal validate "would this rule match?" before activation.
     */
    @Transactional(readOnly = true)
    public RuleEvaluationResponse evaluateSingleRule(String tenantId, String ruleUid, RuleEvaluateRequest request) {
        if (ruleUid == null || ruleUid.isBlank()) {
            return evaluate(tenantId, request);
        }
        Instant now = Instant.now();
        String programmeUid = request.getProgrammeUid() == null || request.getProgrammeUid().isBlank()
            ? "default"
            : request.getProgrammeUid();

        RuleEvaluationResponse.Builder rb = RuleEvaluationResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .customerId(request.getCustomerId())
            .eventId(request.getEventId());

        ObjectNode trace = objectMapper.createObjectNode();
        trace.put("programmeUid", programmeUid);
        trace.put("eventType", request.getEventType());
        trace.put("singleRuleUid", ruleUid);

        try {
            ProgrammeEvaluationContext progCtx = programmeRuleContextLoader.load(tenantId, programmeUid, request.getCustomerTierUid());
            Map<String, Object> eventMap = buildEventMap(request);
            Map<String, Object> customerMap = buildCustomerMap(request, progCtx);
            Map<String, Object> tenantMap = progCtx.asTenantVariableMap();

            EarnRule ruleEntity = earnRuleRepository.loadForAdminEdit(tenantId, programmeUid, ruleUid)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleUid));
            CachedRuleSnapshot snapshot = RuleCacheService.snapshotsFromEntities(List.of(ruleEntity)).getFirst();

            boolean conditionOk = evaluateCondition(snapshot, eventMap, customerMap, tenantMap, now, progCtx);
            if (!conditionOk) {
                trace.put("matched", false);
                return rb
                    .basePointsCalculated(BigDecimal.ZERO)
                    .tierMultiplier(progCtx.getResolvedTierMultiplier())
                    .finalPointsAwarded(BigDecimal.ZERO)
                    .matchedRules(List.of())
                    .suppressedRules(List.of())
                    .rewardCommands(List.of())
                    .success(true)
                    .message("No match")
                    .evaluationTrace(trace)
                    .build();
            }

            BigDecimal baseThisRule = sumAwardPointsBase(snapshot, eventMap, customerMap, tenantMap, now);
            BigDecimal tierMult = progCtx.getResolvedTierMultiplier();
            BigDecimal pointsThisRule = baseThisRule.multiply(tierMult).setScale(4, RoundingMode.HALF_UP);

            List<MatchOutcome> outcomes = List.of(new MatchOutcome(snapshot, baseThisRule, pointsThisRule));
            List<RuleEvaluationResponse.SuppressedRuleInfo> suppressed = new ArrayList<>();
            ConflictResult conflict = applyConflictPolicy(outcomes, progCtx.getConflictDefaultStrategy(), suppressed, trace);

            BigDecimal aggregateBeforeCap = conflict.pointsTotal();
            BigDecimal capped = ruleEarningCapService.clipToCaps(
                tenantId, programmeUid, request.getCustomerId(), aggregateBeforeCap,
                progCtx.getDailyCap(), progCtx.getMonthlyCap(), now
            );

            List<RuleEvaluationResponse.RewardCommand> commands = scaleCommandsToTotal(
                conflict.commands(), capped, tenantId, programmeUid, request
            );

            return rb
                .basePointsCalculated(conflict.baseTotal())
                .tierMultiplier(tierMult)
                .finalPointsAwarded(capped)
                .matchedRules(conflict.matched())
                .suppressedRules(suppressed)
                .rewardCommands(commands)
                .success(true)
                .message("Evaluation complete. " + conflict.matched().size() + " rule(s) matched.")
                .evaluationTrace(trace)
                .build();
        } catch (Exception e) {
            trace.put("error", e.getMessage());
            return rb.success(false)
                .message("Error: " + e.getMessage())
                .evaluationTrace(trace)
                .build();
        }
    }

    private RuleEvaluationResponse evaluateInternal(String tenantId, RuleEvaluateRequest request) {
        Instant now = Instant.now();
        String programmeUid = request.getProgrammeUid() == null || request.getProgrammeUid().isBlank()
            ? "default"
            : request.getProgrammeUid();

        RuleEvaluationResponse.Builder rb = RuleEvaluationResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .customerId(request.getCustomerId())
            .eventId(request.getEventId());

        ObjectNode trace = objectMapper.createObjectNode();
        trace.put("programmeUid", programmeUid);
        trace.put("eventType", request.getEventType());

        try {
            ProgrammeEvaluationContext progCtx = programmeRuleContextLoader.load(tenantId, programmeUid, request.getCustomerTierUid());
            trace.set("conflictPolicy", objectMapper.valueToTree(Map.of(
                "defaultStrategy", progCtx.getConflictDefaultStrategy(),
                "allowRuleOverride", progCtx.isAllowRuleOverride()
            )));

            Map<String, Object> eventMap = buildEventMap(request);
            Map<String, Object> customerMap = buildCustomerMap(request, progCtx);
            Map<String, Object> tenantMap = progCtx.asTenantVariableMap();

            List<CachedRuleSnapshot> snapshots = loadSnapshots(tenantId, programmeUid, request.getEventType(), now);
            trace.put("rulesEvaluated", snapshots.size());

            List<MatchOutcome> outcomes = new ArrayList<>();
            for (CachedRuleSnapshot rule : snapshots) {
                boolean conditionOk = evaluateCondition(rule, eventMap, customerMap, tenantMap, now, progCtx);
                if (!conditionOk) {
                    continue;
                }
                BigDecimal baseThisRule = sumAwardPointsBase(rule, eventMap, customerMap, tenantMap, now);
                BigDecimal tierMult = progCtx.getResolvedTierMultiplier();
                BigDecimal pointsThisRule = baseThisRule.multiply(tierMult).setScale(4, RoundingMode.HALF_UP);
                outcomes.add(new MatchOutcome(rule, baseThisRule, pointsThisRule));

                ExecutionMode mode = parseMode(rule.getExecutionMode());
                if (mode == ExecutionMode.FIRST_MATCH) {
                    trace.put("stoppedAfterRuleDueToFirstMatch", rule.getRuleUid());
                    break;
                }
            }

            List<RuleEvaluationResponse.SuppressedRuleInfo> suppressed = new ArrayList<>();
            ConflictResult conflict = applyConflictPolicy(outcomes, progCtx.getConflictDefaultStrategy(), suppressed, trace);

            BigDecimal tierMult = progCtx.getResolvedTierMultiplier();
            BigDecimal baseTotal = conflict.baseTotal();
            BigDecimal aggregateBeforeCap = conflict.pointsTotal();

            BigDecimal capped = ruleEarningCapService.clipToCaps(
                tenantId, programmeUid, request.getCustomerId(), aggregateBeforeCap,
                progCtx.getDailyCap(), progCtx.getMonthlyCap(), now
            );
            if (capped.compareTo(aggregateBeforeCap) < 0) {
                suppressed.add(RuleEvaluationResponse.SuppressedRuleInfo.builder()
                    .ruleUid("_aggregate")
                    .reason("Programme daily/monthly cap (Redis) reduced award from " + aggregateBeforeCap + " to " + capped)
                    .build());
                trace.put("capClip", aggregateBeforeCap.toPlainString() + " -> " + capped.toPlainString());
            }

            List<RuleEvaluationResponse.RewardCommand> commands = scaleCommandsToTotal(
                conflict.commands(), capped, tenantId, programmeUid, request
            );

            ruleEarningCapService.recordEarned(tenantId, programmeUid, request.getCustomerId(), capped, now);

            BigDecimal finalPoints = capped;
            trace.put("basePointsBeforeTier", baseTotal.toPlainString());
            trace.put("aggregatePointsAfterConflict", aggregateBeforeCap.toPlainString());
            trace.set("matchedRuleUids", objectMapper.valueToTree(
                conflict.matched().stream().map(RuleEvaluationResponse.MatchedRuleInfo::getRuleUid).toList()));
            trace.put("finalPoints", finalPoints.toPlainString());
            trace.put("capsRedisEnabled", rulesProperties.isCapsRedisEnabled());

            BigDecimal dayRem = ruleEarningCapService.dailyRemaining(tenantId, programmeUid, request.getCustomerId(), progCtx.getDailyCap(), now);
            BigDecimal monthRem = ruleEarningCapService.monthlyRemaining(tenantId, programmeUid, request.getCustomerId(), progCtx.getMonthlyCap(), now);

            RuleEvaluationResponse response = rb
                .basePointsCalculated(baseTotal)
                .tierMultiplier(tierMult)
                .finalPointsAwarded(finalPoints)
                .dailyCapRemaining(dayRem != null ? dayRem : progCtx.getDailyCap())
                .monthlyCapRemaining(monthRem != null ? monthRem : progCtx.getMonthlyCap())
                .matchedRules(conflict.matched())
                .suppressedRules(suppressed)
                .rewardCommands(commands)
                .success(true)
                .message("Evaluation complete. " + conflict.matched().size() + " rule(s) matched.")
                .evaluationTrace(trace)
                .build();

            auditWriter.write(tenantId, programmeUid, request.getCustomerId(), request.getEventId(), true, trace);
            return response;
        } catch (Exception e) {
            log.error("Rule evaluation failed tenant={} eventId={}", tenantId, request.getEventId(), e);
            trace.put("error", e.getMessage());
            auditWriter.write(tenantId, programmeUid, request.getCustomerId(), request.getEventId(), false, trace);
            return rb.success(false)
                .message("Error: " + e.getMessage())
                .evaluationTrace(trace)
                .build();
        }
    }

    private record MatchOutcome(CachedRuleSnapshot rule, BigDecimal basePoints, BigDecimal pointsAfterTier) {
    }

    private record ConflictResult(
        List<RuleEvaluationResponse.MatchedRuleInfo> matched,
        List<RuleEvaluationResponse.RewardCommand> commands,
        BigDecimal baseTotal,
        BigDecimal pointsTotal
    ) {
    }

    private ConflictResult applyConflictPolicy(
        List<MatchOutcome> outcomes,
        String strategy,
        List<RuleEvaluationResponse.SuppressedRuleInfo> suppressed,
        ObjectNode trace
    ) {
        if (outcomes.isEmpty()) {
            return new ConflictResult(List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        boolean business = "BEST_FOR_BUSINESS".equalsIgnoreCase(strategy);
        if (!business || outcomes.size() == 1) {
            return buildSumResult(outcomes);
        }

        Optional<BigDecimal> minPositive = outcomes.stream()
            .map(MatchOutcome::pointsAfterTier)
            .filter(p -> p.signum() > 0)
            .min(Comparator.naturalOrder());

        MatchOutcome winner;
        if (minPositive.isEmpty()) {
            winner = outcomes.getFirst();
        } else {
            BigDecimal minPoints = minPositive.get();
            List<MatchOutcome> winners = outcomes.stream()
                .filter(o -> o.pointsAfterTier().compareTo(minPoints) == 0)
                .toList();
            winner = winners.stream()
                .min(Comparator.comparingInt(o -> o.rule().getPriority()))
                .orElse(winners.getFirst());
        }

        for (MatchOutcome o : outcomes) {
            if (o.rule().getRuleUid().equals(winner.rule().getRuleUid())) {
                continue;
            }
            suppressed.add(RuleEvaluationResponse.SuppressedRuleInfo.builder()
                .ruleUid(o.rule().getRuleUid())
                .reason("conflictPolicy=BEST_FOR_BUSINESS: single lowest-liability promotion selected")
                .build());
        }
        trace.put("conflictResolution", "BEST_FOR_BUSINESS_SINGLE_WINNER");
        trace.put("winnerRuleUid", winner.rule().getRuleUid());
        return buildSumResult(List.of(winner));
    }

    private ConflictResult buildSumResult(List<MatchOutcome> outcomes) {
        List<RuleEvaluationResponse.MatchedRuleInfo> matched = new ArrayList<>();
        List<RuleEvaluationResponse.RewardCommand> commands = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal pts = BigDecimal.ZERO;
        for (MatchOutcome o : outcomes) {
            matched.add(RuleEvaluationResponse.MatchedRuleInfo.builder()
                .ruleUid(o.rule().getRuleUid())
                .ruleName(o.rule().getName())
                .priority(o.rule().getPriority())
                .pointsFromThisRule(o.pointsAfterTier())
                .build());
            base = base.add(o.basePoints());
            pts = pts.add(o.pointsAfterTier());
            if (o.pointsAfterTier().compareTo(BigDecimal.ZERO) > 0) {
                commands.add(RuleEvaluationResponse.RewardCommand.builder()
                    .commandId(UUID.randomUUID().toString())
                    .pointsToAward(o.pointsAfterTier())
                    .sourceRuleUid(o.rule().getRuleUid())
                    .build());
            }
        }
        return new ConflictResult(matched, commands, base, pts);
    }

    private List<RuleEvaluationResponse.RewardCommand> scaleCommandsToTotal(
        List<RuleEvaluationResponse.RewardCommand> commands,
        BigDecimal targetTotal,
        String tenantId,
        String programmeUid,
        RuleEvaluateRequest request
    ) {
        BigDecimal sum = commands.stream()
            .map(RuleEvaluationResponse.RewardCommand::getPointsToAward)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.signum() <= 0 || targetTotal.compareTo(sum) == 0) {
            return finalizeCommands(commands, tenantId, programmeUid, request);
        }
        BigDecimal factor = targetTotal.divide(sum, 8, RoundingMode.HALF_UP);
        List<RuleEvaluationResponse.RewardCommand> scaled = new ArrayList<>();
        for (RuleEvaluationResponse.RewardCommand c : commands) {
            BigDecimal p = c.getPointsToAward().multiply(factor).setScale(4, RoundingMode.HALF_UP);
            scaled.add(RuleEvaluationResponse.RewardCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .pointsToAward(p)
                .sourceRuleUid(c.getSourceRuleUid())
                .build());
        }
        return finalizeCommands(scaled, tenantId, programmeUid, request);
    }

    private List<RuleEvaluationResponse.RewardCommand> finalizeCommands(
        List<RuleEvaluationResponse.RewardCommand> partial,
        String tenantId,
        String programmeUid,
        RuleEvaluateRequest request
    ) {
        List<RuleEvaluationResponse.RewardCommand> out = new ArrayList<>();
        for (RuleEvaluationResponse.RewardCommand c : partial) {
            String idemInput = tenantId + "|" + programmeUid + "|" + request.getCustomerId() + "|" + request.getEventId()
                + "|" + c.getSourceRuleUid() + "|AWARD_POINTS";
            out.add(RuleEvaluationResponse.RewardCommand.builder()
                .commandId(c.getCommandId() != null ? c.getCommandId() : UUID.randomUUID().toString())
                .idempotencyKey(sha256Hex(idemInput))
                .tenantId(tenantId)
                .programmeUid(programmeUid)
                .customerId(request.getCustomerId())
                .actionType(ActionType.AWARD_POINTS.name())
                .pointsToAward(c.getPointsToAward())
                .sourceRuleUid(c.getSourceRuleUid())
                .timestamp(System.currentTimeMillis())
                .build());
        }
        return out;
    }

    private List<CachedRuleSnapshot> loadSnapshots(String tenantId, String programmeUid, String eventType, Instant now) {
        Optional<List<CachedRuleSnapshot>> cached = ruleCacheService.get(tenantId, programmeUid, eventType);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<EarnRule> rules = earnRuleRepository.findActiveForEvaluation(
            tenantId, programmeUid, eventType, RuleStatus.ACTIVE, now
        );
        ruleCacheService.put(tenantId, programmeUid, eventType, rules);
        return RuleCacheService.snapshotsFromEntities(rules);
    }

    private boolean evaluateCondition(CachedRuleSnapshot rule, Map<String, Object> eventMap,
                                      Map<String, Object> customerMap, Map<String, Object> tenantMap, Instant now,
                                      ProgrammeEvaluationContext progCtx) {
        try {
            String frag;
            if (rule.getConditionTree() == null || rule.getConditionTree().isNull() || rule.getConditionTree().isMissingNode()) {
                frag = "true";
            } else {
                frag = conditionTreeParser.parseConditionTree(rule.getConditionTree(), progCtx.getAllowedEventPropertyNames());
            }
            return spelEvaluationService.evaluateCondition(frag, eventMap, customerMap, tenantMap, now);
        } catch (ConditionParseException e) {
            log.warn("Condition parse failed ruleUid={}: {}", rule.getRuleUid(), e.getMessage());
            return false;
        }
    }

    private BigDecimal sumAwardPointsBase(CachedRuleSnapshot rule, Map<String, Object> eventMap, Map<String, Object> customerMap,
                                          Map<String, Object> tenantMap, Instant now) {
        BigDecimal sum = BigDecimal.ZERO;
        if (rule.getActions() == null) {
            return sum;
        }
        for (CachedActionSnapshot a : rule.getActions()) {
            if (a.getActionType() == null || !ActionType.AWARD_POINTS.name().equals(a.getActionType())) {
                continue;
            }
            String formula = a.getFormula();
            if (formula == null || formula.isBlank()) {
                formula = "#event['amount'] * #tenant['basePointsRate']";
            }
            BigDecimal base = spelEvaluationService.evaluateFormula(formula, eventMap, customerMap, tenantMap, now);
            sum = sum.add(base);
        }
        return sum;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static ExecutionMode parseMode(String s) {
        if (s == null) {
            return ExecutionMode.ALL_MATCHING;
        }
        try {
            return ExecutionMode.valueOf(s);
        } catch (Exception e) {
            return ExecutionMode.ALL_MATCHING;
        }
    }

    private Map<String, Object> buildEventMap(RuleEvaluateRequest request) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", request.getAmount());
        m.put("eventType", request.getEventType());
        m.put("channel", request.getChannel());
        m.put("merchantId", request.getMerchantId());
        if (request.getEventPayload() != null && request.getEventPayload().isObject()) {
            request.getEventPayload().fields().forEachRemaining(e -> m.put(e.getKey(), jsonScalar(e.getValue())));
        }
        return m;
    }

    private static Object jsonScalar(com.fasterxml.jackson.databind.JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isBoolean()) {
            return n.asBoolean();
        }
        if (n.isInt() || n.isLong()) {
            return n.asLong();
        }
        if (n.isNumber()) {
            return n.decimalValue();
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    private Map<String, Object> buildCustomerMap(RuleEvaluateRequest request, ProgrammeEvaluationContext progCtx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerId", request.getCustomerId());
        m.put("tierUid", request.getCustomerTierUid());
        m.put("tierMultiplier", progCtx.getResolvedTierMultiplier());
        m.put("lifetimeValue", BigDecimal.ZERO);
        return m;
    }
}
