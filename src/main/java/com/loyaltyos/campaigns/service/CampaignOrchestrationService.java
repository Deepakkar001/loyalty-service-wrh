package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.entity.CampaignParticipation;
import com.loyaltyos.campaigns.enums.CampaignExecutionMode;
import com.loyaltyos.campaigns.entity.CampaignResolutionLog;
import com.loyaltyos.campaigns.enums.DropReason;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.model.EvaluationScopeFlags;
import com.loyaltyos.campaigns.model.BudgetDecrementResult;
import com.loyaltyos.campaigns.model.CampaignBuiltAward;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignResolutionResult;
import com.loyaltyos.campaigns.model.DroppedCampaign;
import com.loyaltyos.campaigns.model.EligibilityResult;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.repository.CampaignResolutionLogRepository;
import com.loyaltyos.analytics.model.CustomerTierOutcome;
import com.loyaltyos.analytics.service.CustomerTierOutcomeResolver;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import com.loyaltyos.rewards.dto.RewardIssueRequest;
import com.loyaltyos.rewards.dto.RewardIssueResponse;
import com.loyaltyos.rewards.service.ProgrammeCreditExpiryResolver;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import com.loyaltyos.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.rules.service.ProgrammeEvaluationContext;
import com.loyaltyos.rules.service.ProgrammeRuleContextLoader;
import com.loyaltyos.rules.service.RuleEarningCapService;
import com.loyaltyos.rules.service.RuleEvaluationService;
import com.loyaltyos.onboarding.service.EventSchemaPayloadValidator;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.referrals.dto.ReferralEvaluationResult;
import com.loyaltyos.referrals.dto.ReferralIssuanceLine;
import com.loyaltyos.referrals.dto.ReferralStageRewards;
import com.loyaltyos.referrals.dto.ReferralVoucherGrantLine;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.support.ReferralIssuanceSupport;
import com.loyaltyos.referrals.service.ReferralEvaluationService;
import com.loyaltyos.referrals.service.ReferralProgrammeService;
import com.loyaltyos.referrals.service.ReferralRewardDispatchService;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import com.loyaltyos.voucher.service.VoucherAutoIssueService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CampaignOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(CampaignOrchestrationService.class);

    private final CampaignProperties campaignProperties;
    private final ProgrammeService programmeService;
    private final ProgrammeRuleContextLoader programmeRuleContextLoader;
    private final ObjectMapper objectMapper;
    private final CampaignEligibilityService eligibilityService;
    private final CampaignConflictResolver conflictResolver;
    private final CampaignRewardCommandBuilder rewardCommandBuilder;
    private final CampaignBudgetService budgetService;
    private final RuleEvaluationService ruleEvaluationService;
    private final RuleEarningCapService ruleEarningCapService;
    private final RewardIssuanceService rewardIssuanceService;
    private final ProgrammeCreditExpiryResolver creditExpiryResolver;
    private final CustomerTierOutcomeResolver customerTierOutcomeResolver;
    private final CampaignJsonSupport jsonSupport;
    private final CampaignParticipationRepository participationRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignResolutionLogRepository resolutionLogRepository;
    private final VoucherAutoIssueService voucherAutoIssueService;
    private final ReferralEvaluationService referralEvaluationService;
    private final ReferralProgrammeService referralProgrammeService;
    private final ReferralRewardDispatchService referralRewardDispatchService;
    private final TransactionTemplate transactionTemplate;

    public CampaignOrchestrationService(
        CampaignProperties campaignProperties,
        ProgrammeService programmeService,
        ProgrammeRuleContextLoader programmeRuleContextLoader,
        ObjectMapper objectMapper,
        CampaignEligibilityService eligibilityService,
        CampaignConflictResolver conflictResolver,
        CampaignRewardCommandBuilder rewardCommandBuilder,
        CampaignBudgetService budgetService,
        RuleEvaluationService ruleEvaluationService,
        RuleEarningCapService ruleEarningCapService,
        RewardIssuanceService rewardIssuanceService,
        ProgrammeCreditExpiryResolver creditExpiryResolver,
        CustomerTierOutcomeResolver customerTierOutcomeResolver,
        CampaignJsonSupport jsonSupport,
        CampaignParticipationRepository participationRepository,
        CampaignRepository campaignRepository,
        CampaignResolutionLogRepository resolutionLogRepository,
        VoucherAutoIssueService voucherAutoIssueService,
        ReferralEvaluationService referralEvaluationService,
        ReferralProgrammeService referralProgrammeService,
        ReferralRewardDispatchService referralRewardDispatchService,
        PlatformTransactionManager transactionManager
    ) {
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.programmeRuleContextLoader = Objects.requireNonNull(programmeRuleContextLoader, "programmeRuleContextLoader");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.eligibilityService = Objects.requireNonNull(eligibilityService, "eligibilityService");
        this.conflictResolver = Objects.requireNonNull(conflictResolver, "conflictResolver");
        this.rewardCommandBuilder = Objects.requireNonNull(rewardCommandBuilder, "rewardCommandBuilder");
        this.budgetService = Objects.requireNonNull(budgetService, "budgetService");
        this.ruleEvaluationService = Objects.requireNonNull(ruleEvaluationService, "ruleEvaluationService");
        this.ruleEarningCapService = Objects.requireNonNull(ruleEarningCapService, "ruleEarningCapService");
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.creditExpiryResolver = Objects.requireNonNull(creditExpiryResolver, "creditExpiryResolver");
        this.customerTierOutcomeResolver = Objects.requireNonNull(
            customerTierOutcomeResolver, "customerTierOutcomeResolver"
        );
        this.jsonSupport = Objects.requireNonNull(jsonSupport, "jsonSupport");
        this.participationRepository = Objects.requireNonNull(participationRepository, "participationRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.resolutionLogRepository = Objects.requireNonNull(resolutionLogRepository, "resolutionLogRepository");
        this.voucherAutoIssueService = Objects.requireNonNull(voucherAutoIssueService, "voucherAutoIssueService");
        this.referralEvaluationService = Objects.requireNonNull(referralEvaluationService, "referralEvaluationService");
        this.referralProgrammeService = Objects.requireNonNull(referralProgrammeService, "referralProgrammeService");
        this.referralRewardDispatchService = Objects.requireNonNull(
            referralRewardDispatchService, "referralRewardDispatchService"
        );
        this.transactionTemplate = new TransactionTemplate(
            Objects.requireNonNull(transactionManager, "transactionManager")
        );
    }

    /**
     * Commits points/campaign side effects first, then auto-issues vouchers so balance-cache row locks
     * from earning are not held while {@link VoucherAutoIssueService} debits points (REQUIRES_NEW).
     */
    public LoyaltyEventProcessResponse process(String tenantId, LoyaltyEventProcessRequest request) {
        CoreProcessResult core = transactionTemplate.execute(status -> processEventCore(tenantId, request));
        if (core == null) {
            return null;
        }
        if (core.response().isSuccess() && core.ruleEval() != null) {
            log.info(
                "Applying voucher auto-issue after commit (tenant={}, programme={}, customer={}, eventId={})",
                tenantId,
                core.programmeUid(),
                core.customerId(),
                core.eventId()
            );
            applyVoucherAutoIssue(
                tenantId,
                core.programmeUid(),
                core.customerId(),
                core.eventId(),
                core.ruleEval(),
                core.response()
            );

            // If the voucher issuance debited points in a separate transaction, refresh balance so the
            // integration response reflects the post-redemption balance.
            if (core.response().getVoucherIssuance() != null
                && "SUCCESS".equalsIgnoreCase(core.response().getVoucherIssuance().getStatus())
                && core.response().getVoucherIssuance().getPointsRedeemed() != null
                && core.response().getVoucherIssuance().getPointsRedeemed().signum() > 0) {
                try {
                    core.response().setNewBalance(
                        rewardIssuanceService.getBalance(tenantId, core.programmeUid(), core.customerId()).getBalance()
                    );
                } catch (RuntimeException e) {
                    // Keep original newBalance if balance refresh fails; voucher result still returned.
                    log.warn(
                        "Unable to refresh balance after voucher debit (tenant={}, programme={}, customer={}, eventId={}): {}",
                        tenantId,
                        core.programmeUid(),
                        core.customerId(),
                        core.eventId(),
                        e.getMessage()
                    );
                }
            }
        }
        return core.response();
    }

    CoreProcessResult processEventCore(String tenantId, LoyaltyEventProcessRequest request) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(request, "request");

        Instant now = Instant.now();
        String programmeUid = normalizeProgramme(request.getProgrammeUid());
        String eventId = request.getTransactionId().trim();
        String customerId = request.getCustomerId().trim();

        validateEventPayload(tenantId, programmeUid, request);

        EvaluationScopeFlags scope = EvaluationScopeFlags.parse(request.getEvaluationScope());
        String scopedCampaignUid = normalizeCampaignUid(request.getCampaignUid());
        boolean ruleGatedRuntime = isRuleGatedCampaign(tenantId, scopedCampaignUid);
        if (ruleGatedRuntime && scopedCampaignUid != null) {
            scope = new EvaluationScopeFlags(scope.runCampaigns(), false, scope.runReferral());
        }

        if (campaignProperties.isResolutionLogEnabled()) {
            Optional<CampaignResolutionLog> prior = resolutionLogRepository.findByTenantIdAndEventId(tenantId, eventId);
            if (prior.isPresent()) {
                LoyaltyEventProcessResponse replay = buildResponseFromResolutionLog(
                    tenantId, programmeUid, customerId, eventId, prior.get(), request.getCustomerTierUid()
                );
                return new CoreProcessResult(replay, null, programmeUid, customerId, eventId);
            }
        }

        ProgrammeEvaluationContext progCtx = programmeRuleContextLoader.load(
            tenantId, programmeUid, request.getCustomerTierUid()
        );

        CampaignEventContext eventContext = buildEventContext(request);
        List<DroppedCampaign> allDropped = new ArrayList<>();
        List<Campaign> applying = List.of();
        String resolutionMode = null;
        boolean campaignCapApplied = false;

        if (scope.runCampaigns() && campaignProperties.isEnabled() && !ruleGatedRuntime) {
            EligibilityResult eligibility = scopedCampaignUid != null
                ? eligibilityService.findQualifying(tenantId, programmeUid, eventContext, scopedCampaignUid)
                : eligibilityService.findQualifying(tenantId, programmeUid, eventContext);
            allDropped.addAll(eligibility.dropped());
            CampaignResolutionResult resolution = conflictResolver.resolve(eligibility.qualifying(), eventContext);
            applying = new ArrayList<>(resolution.applying());
            allDropped.addAll(resolution.dropped());
            resolutionMode = resolution.resolutionMode();
            campaignCapApplied = resolution.capApplied();
        }

        RuleEvaluationResponse ruleEval = new RuleEvaluationResponse();
        ruleEval.setSuccess(true);
        ruleEval.setFinalPointsAwarded(BigDecimal.ZERO);
        if (scope.runProgrammeRules() || ruleGatedRuntime) {
            RuleEvaluateRequest ruleReq = buildRuleEvaluateRequest(request, programmeUid, eventId);
            if (ruleGatedRuntime && scopedCampaignUid != null) {
                ruleReq.setCampaignUid(scopedCampaignUid);
            }
            ruleEval = ruleEvaluationService.evaluate(tenantId, ruleReq);
        }

        if (!ruleEval.isSuccess()) {
            LoyaltyEventProcessResponse failure = baseResponse(tenantId, programmeUid, customerId, eventId);
            failure.setSuccess(false);
            failure.setMessage(ruleEval.getMessage());
            failure.setCampaignsDropped(allDropped);
            failure.setRuleEvaluation(ruleEval);
            return new CoreProcessResult(failure, ruleEval, programmeUid, customerId, eventId);
        }

        BigDecimal rulePoints = ruleEval.getFinalPointsAwarded() == null
            ? BigDecimal.ZERO
            : ruleEval.getFinalPointsAwarded();

        boolean rulesMatched = ruleEval.getMatchedRules() != null && !ruleEval.getMatchedRules().isEmpty();
        applying = filterStackableWithRules(applying, rulesMatched, rulePoints, allDropped);

        Instant defaultExpiry = creditExpiryResolver.resolveExpiresAtForCustomer(
            tenantId, programmeUid, customerId, request.getCustomerTierUid(), now
        );
        List<CampaignBuiltAward> builtAwards = ruleGatedRuntime
            ? List.of()
            : rewardCommandBuilder.build(applying, eventContext, eventId, rulePoints, defaultExpiry);

        CapClipResult capClip = clipCampaignPointsToProgrammeCaps(
            tenantId, programmeUid, customerId, builtAwards, progCtx, now
        );
        builtAwards = capClip.awards();
        if (capClip.applied()) {
            campaignCapApplied = true;
        }

        builtAwards = applyBudgetDecrement(tenantId, builtAwards, allDropped);

        if (ruleGatedRuntime && scopedCampaignUid != null && rulePoints.signum() > 0) {
            BudgetDecrementResult budget = budgetService.tryDecrementBudget(tenantId, scopedCampaignUid, rulePoints);
            if (!budget.success()) {
                allDropped.add(new DroppedCampaign(scopedCampaignUid, scopedCampaignUid, DropReason.BUDGET_EXHAUSTED));
                rulePoints = BigDecimal.ZERO;
                ruleEval.setFinalPointsAwarded(BigDecimal.ZERO);
                ruleEval.setRewardCommands(List.of());
            }
        }

        List<RewardIssueCommandDto> issueCommands = new ArrayList<>();
        if (scope.runProgrammeRules() || ruleGatedRuntime) {
            for (RuleEvaluationResponse.RewardCommand rc : ruleEval.getRewardCommands()) {
                issueCommands.add(toIssueCommand(rc));
            }
        }

        BigDecimal referralPointsToEventCustomer = BigDecimal.ZERO;
        BigDecimal referralPointsToOtherCustomers = BigDecimal.ZERO;
        List<ReferralIssuanceLine> deferredReferralLines = new ArrayList<>();
        List<ReferralVoucherGrantLine> deferredReferralVouchers = new ArrayList<>();
        String referralUidForDispatch = null;
        ReferralProgrammeConfig referralConfig = null;
        if (scope.runReferral()) {
            ReferralEvaluationResult referralEval = referralEvaluationService.evaluateEvent(
                tenantId,
                programmeUid,
                customerId,
                eventId,
                request.getEventType(),
                request.getAmount(),
                request.getMetadata()
            );
            referralUidForDispatch = referralEval.getReferralUid();
            ReferralProgramme referralProgramme = referralProgrammeService.getActiveOrNull(tenantId, programmeUid);
            if (referralProgramme != null) {
                referralConfig = referralProgrammeService.readConfig(referralProgramme);
            }
            for (ReferralIssuanceLine line : referralEval.getIssuanceLines()) {
                if (line == null) {
                    continue;
                }
                if (line.getCustomerId() != null && line.getCustomerId().equals(customerId) && line.getCommand() != null) {
                    issueCommands.add(line.getCommand());
                } else if (line.getCustomerId() != null && line.getCommand() != null) {
                    deferredReferralLines.add(line);
                }
            }
            referralPointsToEventCustomer = ReferralIssuanceSupport.sumPointsForCustomer(
                referralEval.getIssuanceLines(), customerId
            );
            referralPointsToOtherCustomers = ReferralIssuanceSupport.sumPointsExcludingCustomer(
                referralEval.getIssuanceLines(), customerId
            );
            if (referralEval.getVoucherGrants() != null) {
                for (ReferralVoucherGrantLine grant : referralEval.getVoucherGrants()) {
                    if (grant != null) {
                        deferredReferralVouchers.add(grant);
                    }
                }
            }
        }

        BigDecimal balanceBeforeIssue = rewardIssuanceService.getBalance(tenantId, programmeUid, customerId).getBalance();

        BigDecimal campaignPoints = BigDecimal.ZERO;
        for (CampaignBuiltAward award : builtAwards) {
            if (award.issueCommand() != null) {
                issueCommands.add(award.issueCommand());
            }
            campaignPoints = campaignPoints.add(award.pointsAwarded());
        }

        RewardIssueRequest issueRequest = new RewardIssueRequest();
        issueRequest.setProgrammeUid(programmeUid);
        issueRequest.setCustomerId(customerId);
        issueRequest.setEventId(eventId);
        issueRequest.setMerchantUid(metadataString(request, "merchantId"));
        issueRequest.setRewardCommands(issueCommands);

        RewardIssueResponse issueResponse = rewardIssuanceService.issue(tenantId, issueRequest);

        if ((!deferredReferralLines.isEmpty() || !deferredReferralVouchers.isEmpty()) && referralConfig != null) {
            ReferralStageRewards deferred = new ReferralStageRewards();
            deferred.setIssuanceLines(deferredReferralLines);
            deferred.setVoucherGrants(deferredReferralVouchers);
            BigDecimal extraReferral = referralRewardDispatchService.dispatchStageRewards(
                tenantId,
                programmeUid,
                referralUidForDispatch != null ? referralUidForDispatch : "unknown",
                eventId,
                referralConfig,
                deferred
            );
            referralPointsToOtherCustomers = referralPointsToOtherCustomers.add(extraReferral);
        }

        if (scope.runCampaigns() || ruleGatedRuntime) {
            if (ruleGatedRuntime && scopedCampaignUid != null && rulePoints.signum() > 0) {
                persistRuleGatedParticipation(tenantId, programmeUid, customerId, eventId, scopedCampaignUid, rulePoints);
            } else {
                persistParticipations(tenantId, programmeUid, customerId, eventId, builtAwards);
            }
            persistResolutionLog(
                tenantId,
                programmeUid,
                customerId,
                eventId,
                eligibilityUids(applying, builtAwards),
                builtAwards,
                allDropped,
                campaignPoints,
                resolutionMode,
                campaignCapApplied
            );
        }

        LoyaltyEventProcessResponse response = baseResponse(tenantId, programmeUid, customerId, eventId);
        response.setSuccess(true);
        response.setMessage(issueResponse.getMessage());
        response.setIdempotentReplay(issueResponse.isIdempotentReplay());
        BigDecimal programmeRulePoints = ruleGatedRuntime ? BigDecimal.ZERO : rulePoints;
        BigDecimal campaignAwarded = ruleGatedRuntime ? rulePoints : campaignPoints;
        response.setRulePointsAwarded(programmeRulePoints);
        response.setCampaignPointsAwarded(campaignAwarded);
        BigDecimal referralPointsTotal = referralPointsToEventCustomer.add(referralPointsToOtherCustomers);
        response.setReferralPointsAwarded(referralPointsTotal);
        response.setReferralPointsToOtherCustomers(referralPointsToOtherCustomers);
        response.setTotalPointsAwarded(programmeRulePoints.add(campaignAwarded).add(referralPointsToEventCustomer));
        response.setPreviousBalance(balanceBeforeIssue);
        response.setNewBalance(issueResponse.getNewBalance());
        if (!deferredReferralLines.isEmpty()) {
            try {
                response.setNewBalance(
                    rewardIssuanceService.getBalance(tenantId, programmeUid, customerId).getBalance()
                );
            } catch (RuntimeException ignored) {
                // keep issue response balance for event customer
            }
        }
        response.setProgrammeCapApplied(capClip.applied());
        response.setResolutionMode(resolutionMode);
        response.setCampaignsApplied(toAppliedLines(builtAwards));
        response.setCampaignsDropped(allDropped);
        applyTierOutcome(
            response,
            customerTierOutcomeResolver.resolve(
                tenantId,
                programmeUid,
                request.getCustomerTierUid(),
                balanceBeforeIssue,
                response.getNewBalance()
            )
        );
        response.setRuleEvaluation(ruleEval);

        return new CoreProcessResult(response, ruleEval, programmeUid, customerId, eventId);
    }

    private static String normalizeCampaignUid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private record CoreProcessResult(
        LoyaltyEventProcessResponse response,
        RuleEvaluationResponse ruleEval,
        String programmeUid,
        String customerId,
        String eventId
    ) {}

    private void applyVoucherAutoIssue(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        RuleEvaluationResponse ruleEval,
        LoyaltyEventProcessResponse response
    ) {
        if (!voucherAutoIssueService.isEnabled()) {
            return;
        }
        if (ruleEval == null || ruleEval.getCatalogGrants() == null || ruleEval.getCatalogGrants().isEmpty()) {
            return;
        }
        Map<String, Integer> priorities = new HashMap<>();
        if (ruleEval.getMatchedRules() != null) {
            for (RuleEvaluationResponse.MatchedRuleInfo m : ruleEval.getMatchedRules()) {
                priorities.put(m.getRuleUid(), m.getPriority() != null ? m.getPriority() : 0);
            }
        }

        // Issue only one: choose best eligible auto-issue grant by priority (higher first).
        Optional<RuleEvaluationResponse.CatalogGrantInfo> chosen = ruleEval.getCatalogGrants().stream()
            .filter(g -> g != null && g.isValid())
            .filter(g -> g.getIssueMode() != null && "AUTO_ISSUE_ON_EVENT".equalsIgnoreCase(g.getIssueMode()))
            .filter(g -> g.getCatalogRewardUid() != null && !g.getCatalogRewardUid().isBlank())
            .sorted(Comparator.comparingInt(
                g -> -(priorities.getOrDefault(g.getSourceRuleUid(), 0))
            ))
            .findFirst();

        if (chosen.isEmpty()) {
            return;
        }

        RuleEvaluationResponse.CatalogGrantInfo grant = chosen.get();
        String redemptionId = eventId + ":" + grant.getSourceRuleUid();

        VoucherIssueResponse issued;
        try {
            issued = voucherAutoIssueService.autoIssue(
                tenantId,
                programmeUid,
                grant.getCatalogRewardUid(),
                customerId,
                redemptionId,
                grant.getPointsToRedeem(),
                grant.getFaceValue()
            );
        } catch (com.loyaltyos.rewards.exception.RewardInsufficientBalanceException e) {
            issued = new VoucherIssueResponse();
            issued.setStatus("INSUFFICIENT_BALANCE");
            issued.setErrorMessage(e.getMessage());
            issued.setRetryable(false);
            issued.setTimestamp(Instant.now());
        } catch (RuntimeException e) {
            issued = new VoucherIssueResponse();
            issued.setStatus("FAILED");
            issued.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Voucher auto-issue failed");
            issued.setRetryable(true);
            issued.setTimestamp(Instant.now());
        }

        LoyaltyEventProcessResponse.VoucherIssuanceResult out = new LoyaltyEventProcessResponse.VoucherIssuanceResult();
        out.setRuleUid(grant.getSourceRuleUid());
        out.setCatalogRewardUid(grant.getCatalogRewardUid());
        out.setStatus(issued != null ? issued.getStatus() : "FAILED");
        out.setErrorMessage(issued != null ? issued.getErrorMessage() : "Voucher auto-issue failed");
        out.setPointsRedeemed(issued != null ? issued.getPointsRedeemed() : null);
        if (issued != null && issued.getSelectedDenomination() != null) {
            out.setSelectedFaceValue(issued.getSelectedDenomination().getFaceValue());
            out.setSelectedCurrency(issued.getSelectedDenomination().getCurrency());
        }
        if (issued != null && issued.getVoucher() != null) {
            out.setCode(issued.getVoucher().getCode());
            out.setPin(issued.getVoucher().getPin());
        }
        response.setVoucherIssuance(out);
    }

    private void validateEventPayload(String tenantId, String programmeUid, LoyaltyEventProcessRequest request) {
        Map<String, Object> payload = buildValidationPayload(request);
        Map<String, String> errors = new LinkedHashMap<>();

        try {
            ProgrammeConfig programmeCfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
            if (programmeCfg != null && programmeCfg.getConfigJson() != null && !programmeCfg.getConfigJson().isBlank()) {
                JsonNode root = objectMapper.readTree(programmeCfg.getConfigJson());
                errors.putAll(EventSchemaPayloadValidator.validatePayload(payload, root));
            } else {
                requireField(payload, "eventType", errors);
                requireField(payload, "transactionId", errors);
                requireField(payload, "customerId", errors);
                requireField(payload, "amount", errors);
            }
        } catch (JsonProcessingException e) {
            errors.put("programmeConfig", "Unable to read programme configuration");
        }

        if (!errors.isEmpty()) {
            throw new CampaignBadRequestException("Event payload validation failed", errors);
        }
    }

    private static void requireField(Map<String, Object> payload, String key, Map<String, String> errors) {
        Object v = payload.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            errors.put(key, key + " is required");
        }
    }

    private Map<String, Object> buildValidationPayload(LoyaltyEventProcessRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("programmeUid", normalizeProgramme(request.getProgrammeUid()));
        payload.put("customerId", request.getCustomerId());
        payload.put("eventType", request.getEventType());
        payload.put("transactionId", request.getTransactionId());
        payload.put("amount", request.getAmount());
        if (request.getCustomerTierUid() != null) {
            payload.put("tierUid", request.getCustomerTierUid());
        }
        if (request.getMetadata() != null) {
            payload.putAll(request.getMetadata());
        }
        if (request.getEventPayload() != null && request.getEventPayload().isObject()) {
            request.getEventPayload().properties().forEach(e ->
                payload.putIfAbsent(e.getKey(), objectMapper.convertValue(e.getValue(), Object.class))
            );
        }
        return payload;
    }

    private static CampaignEventContext buildEventContext(LoyaltyEventProcessRequest request) {
        String channel = null;
        String country = null;
        if (request.getMetadata() != null) {
            Object ch = request.getMetadata().get("channel");
            if (ch != null) {
                channel = String.valueOf(ch);
            }
            Object co = request.getMetadata().get("country");
            if (co != null) {
                country = String.valueOf(co);
            }
        }
        return new CampaignEventContext(
            request.getCustomerId().trim(),
            request.getCustomerTierUid(),
            request.getEventType().trim(),
            request.getAmount(),
            channel,
            country,
            metadataString(request, "merchantId")
        );
    }

    private RuleEvaluateRequest buildRuleEvaluateRequest(
        LoyaltyEventProcessRequest request,
        String programmeUid,
        String eventId
    ) {
        JsonNode payload = request.getEventPayload();
        if (payload == null || payload.isNull()) {
            payload = objectMapper.createObjectNode();
        }
        return RuleEvaluateRequest.builder()
            .programmeUid(programmeUid)
            .customerId(request.getCustomerId().trim())
            .customerTierUid(request.getCustomerTierUid())
            .eventId(eventId)
            .eventType(request.getEventType().trim())
            .amount(request.getAmount())
            .eventPayload(payload)
            .channel(metadataString(request, "channel"))
            .merchantId(metadataString(request, "merchantId"))
            .build();
    }

    private static String metadataString(LoyaltyEventProcessRequest request, String key) {
        if (request.getMetadata() == null) {
            return null;
        }
        Object v = request.getMetadata().get(key);
        return v == null ? null : String.valueOf(v);
    }

    private List<Campaign> filterStackableWithRules(
        List<Campaign> applying,
        boolean rulesMatched,
        BigDecimal rulePoints,
        List<DroppedCampaign> dropped
    ) {
        if (!rulesMatched && rulePoints.signum() <= 0) {
            return applying;
        }
        List<Campaign> kept = new ArrayList<>();
        for (Campaign campaign : applying) {
            CampaignOfferConfig offer = jsonSupport.parseOfferConfig(campaign.getOfferConfig());
            if (offer != null && !offer.isStackableWithRules()) {
                dropped.add(new DroppedCampaign(
                    campaign.getCampaignUid(),
                    campaign.getName(),
                    DropReason.NOT_STACKABLE_CONFLICT
                ));
            } else {
                kept.add(campaign);
            }
        }
        return kept;
    }

    private record CapClipResult(List<CampaignBuiltAward> awards, boolean applied) {}

    private CapClipResult clipCampaignPointsToProgrammeCaps(
        String tenantId,
        String programmeUid,
        String customerId,
        List<CampaignBuiltAward> awards,
        ProgrammeEvaluationContext progCtx,
        Instant now
    ) {
        BigDecimal proposed = awards.stream()
            .map(CampaignBuiltAward::pointsAwarded)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (proposed.signum() <= 0) {
            return new CapClipResult(awards, false);
        }

        BigDecimal clipped = ruleEarningCapService.clipToCaps(
            tenantId,
            programmeUid,
            customerId,
            proposed,
            progCtx.getDailyCap(),
            progCtx.getMonthlyCap(),
            now
        );

        if (clipped.compareTo(proposed) >= 0) {
            if (clipped.signum() > 0) {
                ruleEarningCapService.recordEarned(tenantId, programmeUid, customerId, clipped, now);
            }
            return new CapClipResult(awards, false);
        }

        BigDecimal factor = clipped.divide(proposed, 8, RoundingMode.HALF_UP);
        List<CampaignBuiltAward> scaled = new ArrayList<>();
        for (CampaignBuiltAward award : awards) {
            if (award.pointsAwarded().signum() <= 0) {
                scaled.add(award);
                continue;
            }
            BigDecimal newPoints = award.pointsAwarded().multiply(factor).setScale(4, RoundingMode.HALF_UP);
            if (newPoints.signum() <= 0) {
                continue;
            }
            scaled.add(rescaleAward(award, newPoints));
        }

        if (clipped.signum() > 0) {
            ruleEarningCapService.recordEarned(tenantId, programmeUid, customerId, clipped, now);
        }
        return new CapClipResult(scaled, true);
    }

    private CampaignBuiltAward rescaleAward(CampaignBuiltAward award, BigDecimal newPoints) {
        RewardIssueCommandDto cmd = award.issueCommand();
        if (cmd != null) {
            cmd.setPointsToAward(newPoints);
        }
        BigDecimal budgetCost = newPoints.signum() > 0 ? newPoints : award.cashbackAmount();
        return new CampaignBuiltAward(
            award.campaignUid(),
            award.campaignName(),
            award.offerLine(),
            newPoints,
            award.cashbackAmount(),
            budgetCost,
            cmd
        );
    }

    private List<CampaignBuiltAward> applyBudgetDecrement(
        String tenantId,
        List<CampaignBuiltAward> awards,
        List<DroppedCampaign> dropped
    ) {
        List<CampaignBuiltAward> kept = new ArrayList<>();
        for (CampaignBuiltAward award : awards) {
            if (award.budgetCost().signum() <= 0) {
                kept.add(award);
                continue;
            }
            BudgetDecrementResult budget = budgetService.tryDecrementBudget(
                tenantId, award.campaignUid(), award.budgetCost()
            );
            if (!budget.success()) {
                dropped.add(new DroppedCampaign(
                    award.campaignUid(),
                    award.campaignName(),
                    DropReason.BUDGET_EXHAUSTED
                ));
            } else {
                kept.add(award);
            }
        }
        return kept;
    }

    private void persistParticipations(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        List<CampaignBuiltAward> awards
    ) {
        for (CampaignBuiltAward award : awards) {
            if (!award.hasPoints() && !award.hasCashback()) {
                continue;
            }
            CampaignParticipation row = new CampaignParticipation();
            row.setTenantId(tenantId);
            row.setProgrammeUid(programmeUid);
            row.setCampaignUid(award.campaignUid());
            row.setCustomerId(customerId);
            row.setEventId(eventId);
            row.setPointsAwarded(award.pointsAwarded());
            row.setCashbackAmount(award.cashbackAmount());
            participationRepository.save(row);
        }
    }

    private void persistResolutionLog(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        List<String> evaluatedUids,
        List<CampaignBuiltAward> applied,
        List<DroppedCampaign> dropped,
        BigDecimal campaignPoints,
        String resolutionMode,
        boolean capApplied
    ) {
        if (!campaignProperties.isResolutionLogEnabled()) {
            return;
        }

        CampaignResolutionLog logRow = new CampaignResolutionLog();
        logRow.setTenantId(tenantId);
        logRow.setProgrammeUid(programmeUid);
        logRow.setEventId(eventId);
        logRow.setCustomerId(customerId);
        logRow.setCampaignsEvaluated(toJsonArray(evaluatedUids));
        logRow.setCampaignsApplied(toAppliedJson(applied));
        logRow.setCampaignsDropped(toDroppedJson(dropped));
        logRow.setTotalPointsAwarded(campaignPoints);
        logRow.setResolutionMode(resolutionMode);
        logRow.setCapApplied(capApplied);

        try {
            resolutionLogRepository.save(logRow);
        } catch (DataIntegrityViolationException ex) {
            log.debug(
                "Resolution log already exists for tenant={} eventId={}: {}",
                tenantId,
                eventId,
                ex.getMessage()
            );
        }
    }

    private LoyaltyEventProcessResponse buildResponseFromResolutionLog(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        CampaignResolutionLog logRow,
        String requestedTierUid
    ) {
        BigDecimal balance = rewardIssuanceService.getBalance(tenantId, programmeUid, customerId).getBalance();

        LoyaltyEventProcessResponse response = baseResponse(tenantId, programmeUid, customerId, eventId);
        response.setSuccess(true);
        response.setIdempotentReplay(true);
        response.setMessage("Idempotent replay: event already processed.");
        response.setCampaignPointsAwarded(
            logRow.getTotalPointsAwarded() == null ? BigDecimal.ZERO : logRow.getTotalPointsAwarded()
        );
        response.setTotalPointsAwarded(response.getCampaignPointsAwarded());
        response.setPreviousBalance(balance);
        response.setNewBalance(balance);
        response.setResolutionMode(logRow.getResolutionMode());
        response.setProgrammeCapApplied(logRow.isCapApplied());
        response.setCampaignsApplied(parseAppliedFromLog(logRow.getCampaignsApplied()));
        response.setCampaignsDropped(parseDroppedFromLog(logRow.getCampaignsDropped()));
        applyTierOutcome(
            response,
            customerTierOutcomeResolver.resolve(
                tenantId, programmeUid, requestedTierUid, balance, balance
            )
        );
        return response;
    }

    private static void applyTierOutcome(LoyaltyEventProcessResponse response, CustomerTierOutcome outcome) {
        if (outcome == null) {
            return;
        }
        response.setTierBeforeUid(outcome.tierBeforeUid());
        response.setTierAfterUid(outcome.tierAfterUid());
        response.setTierBeforeName(outcome.tierBeforeName());
        response.setTierAfterName(outcome.tierAfterName());
        response.setTierChanged(outcome.tierChanged());
    }

    private JsonNode toJsonArray(List<String> uids) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (String uid : uids) {
            arr.add(uid);
        }
        return arr;
    }

    private JsonNode toAppliedJson(List<CampaignBuiltAward> applied) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (CampaignBuiltAward award : applied) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("campaignUid", award.campaignUid());
            node.put("campaignName", award.campaignName());
            node.put("offerLine", award.offerLine());
            node.put("pointsAwarded", award.pointsAwarded());
            node.put("cashbackAwarded", award.cashbackAmount());
            arr.add(node);
        }
        return arr;
    }

    private JsonNode toDroppedJson(List<DroppedCampaign> dropped) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (DroppedCampaign d : dropped) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("campaignUid", d.campaignUid());
            node.put("campaignName", d.campaignName());
            node.put("dropReason", d.dropReason().name());
            arr.add(node);
        }
        return arr;
    }

    private static List<String> eligibilityUids(List<Campaign> applying, List<CampaignBuiltAward> built) {
        List<String> uids = new ArrayList<>();
        for (Campaign c : applying) {
            uids.add(c.getCampaignUid());
        }
        for (CampaignBuiltAward a : built) {
            if (!uids.contains(a.campaignUid())) {
                uids.add(a.campaignUid());
            }
        }
        return uids;
    }

    private List<LoyaltyEventProcessResponse.AppliedCampaignLine> toAppliedLines(List<CampaignBuiltAward> awards) {
        List<LoyaltyEventProcessResponse.AppliedCampaignLine> lines = new ArrayList<>();
        for (CampaignBuiltAward award : awards) {
            lines.add(new LoyaltyEventProcessResponse.AppliedCampaignLine(
                award.campaignUid(),
                award.campaignName(),
                award.offerLine(),
                award.pointsAwarded(),
                award.cashbackAmount()
            ));
        }
        return lines;
    }

    private List<LoyaltyEventProcessResponse.AppliedCampaignLine> parseAppliedFromLog(JsonNode node) {
        List<LoyaltyEventProcessResponse.AppliedCampaignLine> lines = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return lines;
        }
        for (JsonNode item : node) {
            lines.add(new LoyaltyEventProcessResponse.AppliedCampaignLine(
                item.path("campaignUid").asText(),
                item.path("campaignName").asText(null),
                item.path("offerLine").asText(null),
                decimalOrZero(item.path("pointsAwarded")),
                decimalOrZero(item.path("cashbackAwarded"))
            ));
        }
        return lines;
    }

    private List<DroppedCampaign> parseDroppedFromLog(JsonNode node) {
        List<DroppedCampaign> dropped = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return dropped;
        }
        for (JsonNode item : node) {
            DropReason reason;
            try {
                reason = DropReason.valueOf(item.path("dropReason").asText("ELIGIBILITY_FAILED"));
            } catch (IllegalArgumentException e) {
                reason = DropReason.ELIGIBILITY_FAILED;
            }
            dropped.add(new DroppedCampaign(
                item.path("campaignUid").asText(),
                item.path("campaignName").asText(null),
                reason
            ));
        }
        return dropped;
    }

    private static BigDecimal decimalOrZero(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText("0"));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean isRuleGatedCampaign(String tenantId, String campaignUid) {
        if (campaignUid == null || campaignUid.isBlank() || !campaignProperties.isEnabled()) {
            return false;
        }
        return campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid.trim())
            .map(c -> c.getExecutionMode() == CampaignExecutionMode.RULE_GATED)
            .orElse(false);
    }

    private void persistRuleGatedParticipation(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        String campaignUid,
        BigDecimal points
    ) {
        CampaignParticipation row = new CampaignParticipation();
        row.setTenantId(tenantId);
        row.setProgrammeUid(programmeUid);
        row.setCampaignUid(campaignUid);
        row.setCustomerId(customerId);
        row.setEventId(eventId);
        row.setPointsAwarded(points);
        row.setCashbackAmount(BigDecimal.ZERO);
        participationRepository.save(row);
    }

    private static RewardIssueCommandDto toIssueCommand(RuleEvaluationResponse.RewardCommand rc) {
        RewardIssueCommandDto dto = new RewardIssueCommandDto();
        dto.setIdempotencyKey(rc.getIdempotencyKey());
        dto.setSourceRuleUid(rc.getSourceRuleUid());
        dto.setSourceCampaignUid(rc.getSourceCampaignUid());
        dto.setPointsToAward(rc.getPointsToAward());
        dto.setActionType(rc.getActionType());
        dto.setCommandId(rc.getCommandId());
        return dto;
    }

    private static LoyaltyEventProcessResponse baseResponse(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId
    ) {
        LoyaltyEventProcessResponse response = new LoyaltyEventProcessResponse();
        response.setTenantId(tenantId);
        response.setProgrammeUid(programmeUid);
        response.setCustomerId(customerId);
        response.setEventId(eventId);
        return response;
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }
}
