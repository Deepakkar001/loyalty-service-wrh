package com.loyaltyos.referrals.service;



import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.loyaltyos.referrals.dto.ReferralIssuanceLine;

import com.loyaltyos.referrals.dto.ReferralStageRewards;

import com.loyaltyos.referrals.dto.ReferralVoucherGrantLine;

import com.loyaltyos.referrals.entity.Referral;

import com.loyaltyos.referrals.entity.ReferralAuditLog;

import com.loyaltyos.referrals.entity.ReferralProgramme;

import com.loyaltyos.referrals.entity.ReferralRewardIssued;

import com.loyaltyos.referrals.enums.ReferralStatus;

import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;

import com.loyaltyos.referrals.model.ReferralPartyRewardConfig;

import com.loyaltyos.referrals.model.ReferralProgrammeConfig;

import com.loyaltyos.referrals.model.ReferralRewardType;

import com.loyaltyos.referrals.model.ReferralStageConfig;

import com.loyaltyos.referrals.repository.ReferralRepository;

import com.loyaltyos.referrals.repository.ReferralRewardIssuedRepository;

import com.loyaltyos.referrals.support.ReferralProgressTracker;

import com.loyaltyos.referrals.support.ReferralProgressTracker.ProgressState;

import com.loyaltyos.referrals.support.ReferralProgressTracker.PurchaseEvent;

import com.loyaltyos.referrals.support.ReferralMilestoneRuleRegistry;
import com.loyaltyos.referrals.support.ReferralRuleEvaluator;
import com.loyaltyos.referrals.support.ReferralStageRewardSupport;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;

import java.math.BigDecimal;

import java.math.RoundingMode;

import java.time.Instant;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Optional;

import java.util.Set;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



@Service

public class ReferralMilestoneService {



    private final ReferralProgrammeService programmeService;

    private final ReferralRepository referralRepository;

    private final ReferralRewardIssuedRepository rewardsIssuedRepository;

    private final ReferralAuditService auditService;

    private final ReferralBudgetService budgetService;

    private final ObjectMapper objectMapper;



    public ReferralMilestoneService(

        ReferralProgrammeService programmeService,

        ReferralRepository referralRepository,

        ReferralRewardIssuedRepository rewardsIssuedRepository,

        ReferralAuditService auditService,

        ReferralBudgetService budgetService,

        ObjectMapper objectMapper

    ) {

        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");

        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");

        this.rewardsIssuedRepository = Objects.requireNonNull(rewardsIssuedRepository, "rewardsIssuedRepository");

        this.auditService = Objects.requireNonNull(auditService, "auditService");

        this.budgetService = Objects.requireNonNull(budgetService, "budgetService");

        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");

    }



    @Transactional

    public ReferralStageRewards completeLinkStage(

        String tenantId,

        String programmeUid,

        Referral referral,

        ReferralStageConfig stage,

        String linkEventKey

    ) {

        if (referral.getStatus() == ReferralStatus.FRAUD_FLAGGED || referral.getStatus() == ReferralStatus.REJECTED) {

            return emptyRewards();

        }

        if (isStageCompleted(referral, stage.getStage())) {

            return emptyRewards();

        }

        ReferralProgramme programme = programmeService.getActiveOrNull(tenantId, programmeUid);

        ReferralProgrammeConfig config = programme != null ? programmeService.readConfig(programme) : new ReferralProgrammeConfig();



        markStageCompleted(referral, stage.getStage(), linkEventKey);

        referralRepository.save(referral);

        auditService.log(

            tenantId,

            programmeUid,

            referral.getReferralUid(),

            ReferralAuditLog.Action.STAGE_COMPLETED,

            ReferralAuditLog.ActorType.SYSTEM,

            referral.getRefereeCustomerId(),

            Map.of("stage", stage.getStage(), "type", stage.getType())

        );

        return buildStageRewards(tenantId, programmeUid, config, referral, stage, linkEventKey);

    }



    @Transactional

    public ReferralStageRewards evaluatePurchaseEvent(

        String tenantId,

        String programmeUid,

        String refereeCustomerId,

        String eventId,

        String eventType,

        BigDecimal amount,

        Map<String, Object> eventMetadata

    ) {

        ReferralProgramme programme = programmeService.getActiveOrNull(tenantId, programmeUid);

        if (programme == null) {

            return emptyRewards();

        }



        Optional<Referral> referralOpt = referralRepository.findByTenantIdAndProgrammeUidAndRefereeCustomerId(

            tenantId, programmeUid, refereeCustomerId

        );

        if (referralOpt.isEmpty()) {

            return emptyRewards();

        }



        Referral referral = referralOpt.get();

        if (referral.getStatus() == ReferralStatus.FRAUD_FLAGGED || referral.getStatus() == ReferralStatus.REJECTED) {

            return emptyRewards();

        }



        if (!"PURCHASE".equalsIgnoreCase(eventType)) {

            return emptyRewards();

        }



        BigDecimal delta = amount != null ? amount : BigDecimal.ZERO;

        Instant now = Instant.now();

        referral.setPurchaseCount(referral.getPurchaseCount() + 1);

        referral.setTotalSpend(

            (referral.getTotalSpend() != null ? referral.getTotalSpend() : BigDecimal.ZERO).add(delta)

        );

        referral.setUpdatedAt(now);

        ReferralProgressTracker.recordPurchase(referral, eventId, delta, now, eventMetadata, objectMapper);



        ReferralProgrammeConfig config = programmeService.readConfig(programme);

        ReferralStageRewards rewards = new ReferralStageRewards();



        for (ReferralStageConfig stage : config.getStages()) {

            ReferralMilestoneRule rule = ReferralMilestoneRuleRegistry.findRule(config, stage.getType()).orElse(null);
            if (rule == null || rule.getTrigger() != ReferralRuleTrigger.PURCHASE) {
                continue;
            }
            if (isStageCompleted(referral, stage.getStage())) {
                continue;
            }
            ProgressState progress = ReferralProgressTracker.load(referral, objectMapper);
            if (!ReferralRuleEvaluator.isSatisfied(rule, referral, progress, now)) {
                continue;
            }

            markStageCompleted(referral, stage.getStage(), eventId);

            auditService.log(

                tenantId,

                programmeUid,

                referral.getReferralUid(),

                ReferralAuditLog.Action.STAGE_COMPLETED,

                ReferralAuditLog.ActorType.SYSTEM,

                refereeCustomerId,

                Map.of("stage", stage.getStage(), "type", stage.getType())

            );

            ReferralStageRewards stageRewards = buildStageRewards(

                tenantId, programmeUid, config, referral, stage, eventId

            );

            rewards.getIssuanceLines().addAll(stageRewards.getIssuanceLines());

            rewards.getVoucherGrants().addAll(stageRewards.getVoucherGrants());

        }



        if (referral.getStatus() == ReferralStatus.SIGNED_UP

            && (!rewards.getIssuanceLines().isEmpty() || !rewards.getVoucherGrants().isEmpty())) {

            referral.setStatus(ReferralStatus.REWARDED);

        }

        referralRepository.save(referral);

        return rewards;

    }

    @Transactional
    public ReferralStageRewards evaluateIntegrationEvent(
        String tenantId,
        String programmeUid,
        String refereeCustomerId,
        String eventId,
        String eventType,
        Map<String, Object> eventMetadata
    ) {
        ReferralProgramme programme = programmeService.getActiveOrNull(tenantId, programmeUid);
        if (programme == null) {
            return emptyRewards();
        }
        if (eventType == null || eventType.isBlank() || "PURCHASE".equalsIgnoreCase(eventType)) {
            return emptyRewards();
        }

        Optional<Referral> referralOpt = referralRepository.findByTenantIdAndProgrammeUidAndRefereeCustomerId(
            tenantId, programmeUid, refereeCustomerId
        );
        if (referralOpt.isEmpty()) {
            return emptyRewards();
        }

        Referral referral = referralOpt.get();
        if (referral.getStatus() == ReferralStatus.FRAUD_FLAGGED || referral.getStatus() == ReferralStatus.REJECTED) {
            return emptyRewards();
        }

        ReferralProgrammeConfig config = programmeService.readConfig(programme);
        ReferralStageRewards rewards = new ReferralStageRewards();
        Instant now = Instant.now();

        for (ReferralStageConfig stage : config.getStages()) {
            ReferralMilestoneRule rule = ReferralMilestoneRuleRegistry.findRule(config, stage.getType()).orElse(null);
            if (rule == null || rule.getTrigger() != ReferralRuleTrigger.INTEGRATION_EVENT) {
                continue;
            }
            if (isStageCompleted(referral, stage.getStage())) {
                continue;
            }
            if (!ReferralRuleEvaluator.isSatisfiedForIntegrationEvent(rule, eventType, eventMetadata)) {
                continue;
            }

            markStageCompleted(referral, stage.getStage(), eventId);
            auditService.log(
                tenantId,
                programmeUid,
                referral.getReferralUid(),
                ReferralAuditLog.Action.STAGE_COMPLETED,
                ReferralAuditLog.ActorType.SYSTEM,
                refereeCustomerId,
                Map.of("stage", stage.getStage(), "type", stage.getType(), "eventType", eventType)
            );
            ReferralStageRewards stageRewards = buildStageRewards(tenantId, programmeUid, config, referral, stage, eventId);
            rewards.getIssuanceLines().addAll(stageRewards.getIssuanceLines());
            rewards.getVoucherGrants().addAll(stageRewards.getVoucherGrants());
        }

        if (referral.getStatus() == ReferralStatus.SIGNED_UP
            && (!rewards.getIssuanceLines().isEmpty() || !rewards.getVoucherGrants().isEmpty())) {
            referral.setStatus(ReferralStatus.REWARDED);
        }
        referral.setUpdatedAt(now);
        referralRepository.save(referral);
        return rewards;
    }

    private static ReferralStageRewards emptyRewards() {

        return new ReferralStageRewards();

    }



    private ReferralStageRewards buildStageRewards(

        String tenantId,

        String programmeUid,

        ReferralProgrammeConfig config,

        Referral referral,

        ReferralStageConfig stage,

        String eventId

    ) {

        ReferralStageRewards rewards = new ReferralStageRewards();



        ReferralPartyRewardConfig referrer = ReferralStageRewardSupport.resolveReferrerReward(stage);

        appendPartyReward(

            rewards,

            tenantId,

            programmeUid,

            config,

            referral,

            stage,

            eventId,

            ReferralRewardIssued.RecipientType.REFERRER,

            referral.getReferrerCustomerId(),

            referrer

        );



        ReferralPartyRewardConfig referee = ReferralStageRewardSupport.resolveRefereeReward(stage);

        appendPartyReward(

            rewards,

            tenantId,

            programmeUid,

            config,

            referral,

            stage,

            eventId,

            ReferralRewardIssued.RecipientType.REFEREE,

            referral.getRefereeCustomerId(),

            referee

        );



        return rewards;

    }



    private void appendPartyReward(

        ReferralStageRewards rewards,

        String tenantId,

        String programmeUid,

        ReferralProgrammeConfig config,

        Referral referral,

        ReferralStageConfig stage,

        String eventId,

        ReferralRewardIssued.RecipientType recipientType,

        String recipientCustomerId,

        ReferralPartyRewardConfig partyReward

    ) {

        if (partyReward == null) {

            return;

        }

        if (recipientType == ReferralRewardIssued.RecipientType.REFERRER

            && !referrerStageCapAllows(tenantId, programmeUid, referral.getReferrerCustomerId(), stage)) {

            return;

        }

        if (partyReward.getType() == ReferralRewardType.VOUCHER) {

            ReferralVoucherGrantLine grant = voucherGrantIfNew(

                tenantId, programmeUid, config, referral, stage, eventId, recipientType, recipientCustomerId, partyReward

            );

            if (grant != null) {

                rewards.getVoucherGrants().add(grant);

            }

            return;

        }

        BigDecimal points = partyReward.getPoints() != null ? partyReward.getPoints() : BigDecimal.ZERO;

        if (points.signum() > 0) {

            ReferralIssuanceLine cmd = pointsCommandIfNew(

                tenantId, programmeUid, config, referral, stage, eventId, recipientType, recipientCustomerId, points

            );

            if (cmd != null) {

                rewards.getIssuanceLines().add(cmd);

            }

        }

    }



    private boolean referrerStageCapAllows(

        String tenantId,

        String programmeUid,

        String referrerCustomerId,

        ReferralStageConfig stage

    ) {

        if (stage.getMaxReferrerAwardsForStage() == null || stage.getMaxReferrerAwardsForStage() < 1) {

            return true;

        }

        long count = rewardsIssuedRepository.countByTenantIdAndProgrammeUidAndRecipientCustomerIdAndStageAndRecipientType(

            tenantId,

            programmeUid,

            referrerCustomerId,

            stage.getStage(),

            ReferralRewardIssued.RecipientType.REFERRER

        );

        return count < stage.getMaxReferrerAwardsForStage();

    }



    private ReferralIssuanceLine pointsCommandIfNew(

        String tenantId,

        String programmeUid,

        ReferralProgrammeConfig config,

        Referral referral,

        ReferralStageConfig stage,

        String eventId,

        ReferralRewardIssued.RecipientType recipientType,

        String recipientCustomerId,

        BigDecimal points

    ) {

        String idempotencyKey = idempotencyKey(referral, stage, recipientType);



        if (rewardsIssuedRepository.findByTenantIdAndRecipientCustomerIdAndIdempotencyKey(

            tenantId, recipientCustomerId, idempotencyKey

        ).isPresent()) {

            return null;

        }



        budgetService.assertWithinBudget(tenantId, programmeUid, config, points);



        ReferralRewardIssued issued = new ReferralRewardIssued();

        issued.setTenantId(tenantId);

        issued.setProgrammeUid(programmeUid);

        issued.setReferralUid(referral.getReferralUid());

        issued.setStage(stage.getStage());

        issued.setRecipientType(recipientType);

        issued.setRewardType(ReferralRewardType.POINTS.name());

        issued.setRecipientCustomerId(recipientCustomerId);

        issued.setPointsAwarded(points.setScale(4, RoundingMode.HALF_UP));

        issued.setIdempotencyKey(idempotencyKey);

        issued.setCreatedAt(Instant.now());

        rewardsIssuedRepository.save(issued);



        auditService.log(

            tenantId,

            programmeUid,

            referral.getReferralUid(),

            ReferralAuditLog.Action.REWARD_ISSUED,

            ReferralAuditLog.ActorType.SYSTEM,

            recipientCustomerId,

            Map.of(

                "stage", stage.getStage(),

                "points", points,

                "recipientType", recipientType.name(),

                "rewardType", "POINTS",

                "eventId", eventId

            )

        );



        RewardIssueCommandDto cmd = new RewardIssueCommandDto();

        cmd.setIdempotencyKey(idempotencyKey);

        cmd.setPointsToAward(points.setScale(4, RoundingMode.HALF_UP));

        cmd.setActionType("AWARD_POINTS");

        cmd.setSourceRuleUid("referral:" + referral.getReferralUid());

        return new ReferralIssuanceLine(recipientCustomerId, cmd);

    }



    private ReferralVoucherGrantLine voucherGrantIfNew(

        String tenantId,

        String programmeUid,

        ReferralProgrammeConfig config,

        Referral referral,

        ReferralStageConfig stage,

        String eventId,

        ReferralRewardIssued.RecipientType recipientType,

        String recipientCustomerId,

        ReferralPartyRewardConfig partyReward

    ) {

        if (partyReward.getCatalogRewardUid() == null || partyReward.getCatalogRewardUid().isBlank()) {

            return null;

        }

        String idempotencyKey = idempotencyKey(referral, stage, recipientType);

        if (rewardsIssuedRepository.findByTenantIdAndRecipientCustomerIdAndIdempotencyKey(

            tenantId, recipientCustomerId, idempotencyKey

        ).isPresent()) {

            return null;

        }



        BigDecimal funding = ReferralVoucherRewardService.resolveFundingPoints(partyReward);

        budgetService.assertWithinBudget(tenantId, programmeUid, config, funding);



        ReferralVoucherGrantLine grant = new ReferralVoucherGrantLine();

        grant.setCustomerId(recipientCustomerId);

        grant.setCatalogRewardUid(partyReward.getCatalogRewardUid().trim());

        grant.setRedemptionId(idempotencyKey);

        grant.setPointsToRedeem(partyReward.getVoucherPointsToRedeem());

        grant.setFaceValue(partyReward.getVoucherFaceValue());

        grant.setFundingPoints(funding);

        grant.setStage(stage.getStage());

        grant.setRecipientType(recipientType.name());

        return grant;

    }



    private static String idempotencyKey(

        Referral referral,

        ReferralStageConfig stage,

        ReferralRewardIssued.RecipientType recipientType

    ) {

        return "referral:" + referral.getReferralUid()

            + ":stage:" + stage.getStage()

            + ":" + recipientType.name().toLowerCase();

    }



    private boolean isStageCompleted(Referral referral, int stage) {

        return loadCompletedStages(referral).contains(stage);

    }



    private void markStageCompleted(Referral referral, int stage, String eventKey) {

        Set<Integer> completed = loadCompletedStages(referral);

        completed.add(stage);

        referral.setCurrentStage(stage);

        try {

            referral.setCompletedStagesJson(objectMapper.writeValueAsString(completed));

        } catch (Exception e) {

            referral.setCompletedStagesJson("[" + stage + "]");

        }

        referral.setUpdatedAt(Instant.now());

    }



    private Set<Integer> loadCompletedStages(Referral referral) {

        if (referral.getCompletedStagesJson() == null || referral.getCompletedStagesJson().isBlank()) {

            return new HashSet<>();

        }

        try {

            List<Integer> list = objectMapper.readValue(

                referral.getCompletedStagesJson(),

                new TypeReference<List<Integer>>() {}

            );

            return new HashSet<>(list);

        } catch (Exception e) {

            return new HashSet<>();

        }

    }

}


