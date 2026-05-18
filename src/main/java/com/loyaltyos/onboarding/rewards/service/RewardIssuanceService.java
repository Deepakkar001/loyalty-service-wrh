package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.analytics.service.TierHistoryService;
import com.loyaltyos.onboarding.analytics.service.TierResolver;
import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.entity.PointsLedger;
import com.loyaltyos.onboarding.rules.enums.ActionType;
import com.loyaltyos.onboarding.rules.enums.LedgerEntryType;
import com.loyaltyos.onboarding.rules.repository.EarnRuleRepository;
import com.loyaltyos.onboarding.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueCommandDto;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueRequest;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueResponse;
import com.loyaltyos.onboarding.rewards.config.RewardEngineProperties;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCache;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCacheId;
import com.loyaltyos.onboarding.rewards.exception.RewardIssuanceValidationException;
import com.loyaltyos.onboarding.rewards.exception.RewardPartialIdempotencyException;
import com.loyaltyos.onboarding.rewards.repository.CustomerBalanceCacheRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RewardIssuanceService {

    private static final int MAX_POINTS_SCALE = 4;

    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheRepository balanceCacheRepository;
    private final EarnRuleRepository earnRuleRepository;
    private final RewardIssuanceAuditService rewardIssuanceAuditService;
    private final RewardEngineProperties rewardEngineProperties;
    private final TierResolver tierResolver;
    private final TierHistoryService tierHistoryService;

    public RewardIssuanceService(
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheRepository balanceCacheRepository,
        EarnRuleRepository earnRuleRepository,
        RewardIssuanceAuditService rewardIssuanceAuditService,
        RewardEngineProperties rewardEngineProperties,
        TierResolver tierResolver,
        TierHistoryService tierHistoryService
    ) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.balanceCacheRepository = Objects.requireNonNull(balanceCacheRepository, "balanceCacheRepository");
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
        this.rewardIssuanceAuditService = Objects.requireNonNull(rewardIssuanceAuditService, "rewardIssuanceAuditService");
        this.rewardEngineProperties = Objects.requireNonNull(rewardEngineProperties, "rewardEngineProperties");
        this.tierResolver = Objects.requireNonNull(tierResolver, "tierResolver");
        this.tierHistoryService = Objects.requireNonNull(tierHistoryService, "tierHistoryService");
    }

    @Transactional(readOnly = true)
    public RewardBalanceResponse getBalance(String tenantId, String programmeUid, String customerId) {
        String p = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid;
        CustomerBalanceCacheId id = new CustomerBalanceCacheId(tenantId, p, customerId);
        BigDecimal ledgerSum = pointsLedgerRepository.sumSignedPointsForCustomer(tenantId, p, customerId);
        if (ledgerSum == null) {
            ledgerSum = BigDecimal.ZERO;
        }
        final BigDecimal ledgerDerived = ledgerSum;
        return balanceCacheRepository.findById(id)
            .map(row -> toBalanceResponse(tenantId, p, customerId, row.getBalance(), row.getUpdatedAt(), ledgerDerived))
            .orElseGet(() -> toBalanceResponse(tenantId, p, customerId, BigDecimal.ZERO, null, ledgerDerived));
    }

    private static RewardBalanceResponse toBalanceResponse(
        String tenantId,
        String programmeUid,
        String customerId,
        BigDecimal cachedBalance,
        Instant updatedAt,
        BigDecimal ledgerDerived
    ) {
        BigDecimal cache = cachedBalance == null ? BigDecimal.ZERO : cachedBalance;
        RewardBalanceResponse r = new RewardBalanceResponse();
        r.setTenantId(tenantId);
        r.setProgrammeUid(programmeUid);
        r.setCustomerId(customerId);
        r.setBalance(cache);
        r.setUpdatedAt(updatedAt);
        r.setLedgerDerivedBalance(ledgerDerived);
        r.setBalanceVariance(ledgerDerived.subtract(cache));
        return r;
    }

    /**
     * Persists CREDIT rows from rule-engine {@code RewardCommand}s and bumps balance cache atomically.
     * Idempotent: full duplicate request returns prior outcome. Partial key overlap fails fast (data integrity).
     */
    @Transactional
    public RewardIssueResponse issue(String tenantId, RewardIssueRequest request) {
        long started = System.currentTimeMillis();
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(request, "request");

        String programmeUid = request.getProgrammeUid() == null || request.getProgrammeUid().isBlank()
            ? "default"
            : request.getProgrammeUid();
        String customerId = request.getCustomerId();
        String eventId = request.getEventId();
        List<RewardIssueCommandDto> commands = request.getRewardCommands();

        RewardIssueResponse response = new RewardIssueResponse();
        response.setTenantId(tenantId);
        response.setProgrammeUid(programmeUid);
        response.setCustomerId(customerId);
        response.setEventId(eventId);
        response.setProcessedAt(Instant.now());

        try {
            if (commands == null || commands.isEmpty()) {
                response.setTotalPointsIssued(BigDecimal.ZERO);
                response.setLedgerRowsCreated(0);
                response.setNewBalance(readBalance(tenantId, programmeUid, customerId));
                response.setIdempotentReplay(false);
                response.setMessage("No reward commands; nothing to issue.");
                return response;
            }

            validateCommands(commands);

            List<String> keys = commands.stream().map(RewardIssueCommandDto::getIdempotencyKey).sorted().toList();
            List<PointsLedger> existing = pointsLedgerRepository.findByTenantIdAndCustomerIdAndIdempotencyKeyIn(
                tenantId, customerId, keys
            );

            if (!existing.isEmpty() && existing.size() == keys.size()) {
                return buildIdempotentSuccess(tenantId, programmeUid, customerId, eventId, existing, response);
            }
            if (!existing.isEmpty()) {
                throw new RewardPartialIdempotencyException(
                    "Partial idempotency: " + existing.size() + " of " + keys.size()
                        + " ledger rows already exist for this customer. Refuse to issue remainder."
                );
            }

            BigDecimal total = BigDecimal.ZERO;
            List<PointsLedger> toSave = new ArrayList<>();
            for (RewardIssueCommandDto cmd : commands.stream()
                .sorted((a, b) -> a.getIdempotencyKey().compareTo(b.getIdempotencyKey()))
                .toList()) {

                BigDecimal pts = cmd.getPointsToAward().setScale(MAX_POINTS_SCALE, RoundingMode.HALF_UP);
                total = total.add(pts);

                Long rulePk = null;
                String campaignUid = normalizeBlank(cmd.getSourceCampaignUid());
                if (campaignUid == null) {
                    rulePk = earnRuleRepository
                        .findByTenantIdAndProgrammeUidAndRuleUid(tenantId, programmeUid, cmd.getSourceRuleUid())
                        .map(EarnRule::getId)
                        .orElse(null);
                }

                String description = buildDescription(eventId, cmd, request.getNarrative());

                Instant creditExpiresAt = cmd.getExpiresAt() != null ? cmd.getExpiresAt() : defaultCreditExpiresAt();

                PointsLedger row = PointsLedger.builder()
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .programmeUid(programmeUid)
                    .idempotencyKey(cmd.getIdempotencyKey())
                    .entryType(LedgerEntryType.CREDIT)
                    .points(pts)
                    .sourceRuleId(rulePk)
                    .sourceEventId(eventId)
                    .sourceCampaignId(campaignUid)
                    .expiresAt(creditExpiresAt)
                    .description(description)
                    .createdBy("REWARD_ENGINE")
                    .build();
                toSave.add(row);
            }

            List<PointsLedger> saved = pointsLedgerRepository.saveAll(toSave);
            pointsLedgerRepository.flush();

            BigDecimal balanceBefore = readBalance(tenantId, programmeUid, customerId);
            var previousTier = tierResolver.resolveTierForBalance(tenantId, programmeUid, balanceBefore);

            balanceCacheRepository.incrementBalance(tenantId, programmeUid, customerId, total);

            BigDecimal balanceAfter = balanceBefore.add(total);
            var newTier = tierResolver.resolveTierForBalance(tenantId, programmeUid, balanceAfter);
            tierHistoryService.recordIfTierChanged(
                tenantId,
                programmeUid,
                customerId,
                previousTier,
                newTier,
                balanceAfter
            );

            Map<String, RewardIssueCommandDto> byKey = new HashMap<>();
            for (RewardIssueCommandDto c : commands) {
                byKey.put(c.getIdempotencyKey(), c);
            }
            List<RewardIssueResponse.IssuedLedgerLine> lines = new ArrayList<>();
            for (PointsLedger row : saved) {
                RewardIssueCommandDto cmd = byKey.get(row.getIdempotencyKey());
                String ruleUid = cmd != null ? sourceLabel(cmd, row) : labelFromLedgerRow(row);
                lines.add(new RewardIssueResponse.IssuedLedgerLine(
                    row.getId(),
                    row.getIdempotencyKey(),
                    row.getPoints(),
                    ruleUid
                ));
            }

            response.setTotalPointsIssued(total);
            response.setLedgerRowsCreated(saved.size());
            response.setNewBalance(readBalance(tenantId, programmeUid, customerId));
            response.setIdempotentReplay(false);
            response.setLedgerLines(lines);
            response.setMessage("Issued " + saved.size() + " CREDIT row(s).");

            int duration = elapsedMs(started);
            List<Long> ledgerIds = saved.stream().map(PointsLedger::getId).toList();
            rewardIssuanceAuditService.recordSuccess(
                tenantId,
                programmeUid,
                customerId,
                eventId,
                total,
                commands.size(),
                ledgerIds,
                duration
            );
            return response;
        } catch (RuntimeException ex) {
            int duration = elapsedMs(started);
            rewardIssuanceAuditService.recordFailure(
                tenantId,
                programmeUid,
                customerId,
                eventId != null ? eventId : "",
                ex.getMessage(),
                duration
            );
            throw ex;
        }
    }

    private static int elapsedMs(long started) {
        long elapsed = System.currentTimeMillis() - started;
        return (int) Math.min(Integer.MAX_VALUE, elapsed);
    }

    private static void validateCommands(List<RewardIssueCommandDto> commands) {
        Set<String> seen = new HashSet<>();
        for (RewardIssueCommandDto cmd : commands) {
            if (!seen.add(cmd.getIdempotencyKey())) {
                throw new RewardIssuanceValidationException("Duplicate idempotencyKey in request: " + cmd.getIdempotencyKey());
            }
            if (cmd.getIdempotencyKey().length() > 128) {
                throw new RewardIssuanceValidationException("idempotencyKey exceeds 128 characters.");
            }
            String campaignUid = normalizeBlank(cmd.getSourceCampaignUid());
            String ruleUid = normalizeBlank(cmd.getSourceRuleUid());
            if (campaignUid == null && ruleUid == null) {
                throw new RewardIssuanceValidationException("Each command must set sourceRuleUid or sourceCampaignUid.");
            }
            if (campaignUid != null && ruleUid != null) {
                throw new RewardIssuanceValidationException("Command cannot set both sourceRuleUid and sourceCampaignUid.");
            }
            String actionType = cmd.getActionType() == null ? ActionType.AWARD_POINTS.name() : cmd.getActionType();
            if (!ActionType.AWARD_POINTS.name().equals(actionType)) {
                throw new RewardIssuanceValidationException("Only actionType AWARD_POINTS is supported; got: " + actionType);
            }
            if (cmd.getPointsToAward() == null || cmd.getPointsToAward().signum() <= 0) {
                throw new RewardIssuanceValidationException("pointsToAward must be positive.");
            }
            if (cmd.getPointsToAward().scale() > MAX_POINTS_SCALE) {
                throw new RewardIssuanceValidationException("pointsToAward scale must be <= " + MAX_POINTS_SCALE);
            }
        }
    }

    private RewardIssueResponse buildIdempotentSuccess(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        List<PointsLedger> existing,
        RewardIssueResponse response
    ) {
        BigDecimal total = existing.stream().map(PointsLedger::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<RewardIssueResponse.IssuedLedgerLine> lines = existing.stream()
            .map(row -> new RewardIssueResponse.IssuedLedgerLine(
                row.getId(),
                row.getIdempotencyKey(),
                row.getPoints(),
                ruleUidFromLedger(row)
            ))
            .collect(Collectors.toList());

        response.setTotalPointsIssued(total);
        response.setLedgerRowsCreated(0);
        response.setNewBalance(readBalance(tenantId, programmeUid, customerId));
        response.setIdempotentReplay(true);
        response.setLedgerLines(lines);
        response.setMessage("Idempotent replay: all " + existing.size() + " ledger row(s) already existed for event " + eventId + ".");
        return response;
    }

    private BigDecimal readBalance(String tenantId, String programmeUid, String customerId) {
        CustomerBalanceCacheId id = new CustomerBalanceCacheId(tenantId, programmeUid, customerId);
        return balanceCacheRepository.findById(id)
            .map(CustomerBalanceCache::getBalance)
            .orElse(BigDecimal.ZERO);
    }

    private Instant defaultCreditExpiresAt() {
        int months = rewardEngineProperties.getDefaultCreditExpiryMonths();
        if (months <= 0) {
            return null;
        }
        return Instant.now().atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }

    private static String buildDescription(String eventId, RewardIssueCommandDto cmd, String narrative) {
        String base;
        String campaignUid = normalizeBlank(cmd.getSourceCampaignUid());
        if (campaignUid != null) {
            base = "CREDIT AWARD_POINTS event=" + eventId + " campaign=" + campaignUid;
        } else {
            base = "CREDIT AWARD_POINTS event=" + eventId + " rule=" + cmd.getSourceRuleUid();
        }
        if (narrative != null && !narrative.isBlank()) {
            return base + " | " + narrative.trim();
        }
        return base;
    }

    private String sourceLabel(RewardIssueCommandDto cmd, PointsLedger row) {
        String campaignUid = normalizeBlank(cmd.getSourceCampaignUid());
        if (campaignUid != null) {
            return campaignUid;
        }
        if (cmd.getSourceRuleUid() != null && !cmd.getSourceRuleUid().isBlank()) {
            return cmd.getSourceRuleUid();
        }
        return labelFromLedgerRow(row);
    }

    private String ruleUidFromLedger(PointsLedger row) {
        return labelFromLedgerRow(row);
    }

    private String labelFromLedgerRow(PointsLedger row) {
        if (row.getSourceCampaignId() != null && !row.getSourceCampaignId().isBlank()) {
            return row.getSourceCampaignId();
        }
        Long ruleId = row.getSourceRuleId();
        if (ruleId == null) {
            return null;
        }
        return earnRuleRepository.findById(ruleId).map(EarnRule::getRuleUid).orElse(null);
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
