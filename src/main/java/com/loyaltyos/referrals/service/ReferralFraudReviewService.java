package com.loyaltyos.referrals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.dto.ReferralFraudQueueItemResponse;
import com.loyaltyos.referrals.dto.ReferralStageRewards;
import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.entity.ReferralAuditLog;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.enums.ReferralStatus;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.support.ReferralMilestoneRuleRegistry;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.referrals.service.ReferralFraudService.FraudCheckResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralFraudReviewService {

    private final ReferralRepository referralRepository;
    private final ReferralProgrammeService programmeService;
    private final ReferralMilestoneService milestoneService;
    private final ReferralRewardDispatchService rewardDispatchService;
    private final ReferralAuditService auditService;
    private final ObjectMapper objectMapper;

    public ReferralFraudReviewService(
        ReferralRepository referralRepository,
        ReferralProgrammeService programmeService,
        ReferralMilestoneService milestoneService,
        ReferralRewardDispatchService rewardDispatchService,
        ReferralAuditService auditService,
        ObjectMapper objectMapper
    ) {
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.milestoneService = Objects.requireNonNull(milestoneService, "milestoneService");
        this.rewardDispatchService = Objects.requireNonNull(rewardDispatchService, "rewardDispatchService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional(readOnly = true)
    public List<ReferralFraudQueueItemResponse> listFraudQueue(String tenantId, String programmeUid) {
        String programme = normalizeProgramme(programmeUid);
        return referralRepository.findByTenantIdAndProgrammeUidAndStatusOrderByCreatedAtDesc(
            tenantId, programme, ReferralStatus.FRAUD_FLAGGED
        ).stream()
            .map(this::toQueueItem)
            .toList();
    }

    @Transactional
    public void approve(String tenantId, String programmeUid, String referralUid, String note) {
        review(tenantId, programmeUid, referralUid, ReferralAuditLog.Action.APPROVED, note, true);
    }

    @Transactional
    public void override(String tenantId, String programmeUid, String referralUid, String note) {
        review(tenantId, programmeUid, referralUid, ReferralAuditLog.Action.OVERRIDE, note, true);
    }

    @Transactional
    public void reject(String tenantId, String programmeUid, String referralUid, String note) {
        review(tenantId, programmeUid, referralUid, ReferralAuditLog.Action.REJECTED, note, false);
    }

    private void review(
        String tenantId,
        String programmeUid,
        String referralUid,
        ReferralAuditLog.Action action,
        String note,
        boolean approve
    ) {
        String programme = normalizeProgramme(programmeUid);
        Referral referral = referralRepository.findByTenantIdAndProgrammeUidAndReferralUid(
            tenantId, programme, referralUid
        ).orElseThrow(() -> new ReferralException("REFERRAL_NOT_FOUND", "Referral not found"));

        if (referral.getStatus() != ReferralStatus.FRAUD_FLAGGED) {
            throw new ReferralException("INVALID_STATUS", "Referral is not in fraud review queue");
        }

        if (!approve) {
            referral.setStatus(ReferralStatus.REJECTED);
            referral.setUpdatedAt(Instant.now());
            referralRepository.save(referral);
            auditService.log(
                tenantId,
                programme,
                referralUid,
                action,
                ReferralAuditLog.ActorType.ADMIN,
                referral.getRefereeCustomerId(),
                Map.of("note", note != null ? note : "")
            );
            return;
        }

        ReferralProgramme active = programmeService.getActiveOrNull(tenantId, programme);
        if (active == null) {
            throw new ReferralException("REFERRAL_PROGRAMME_INACTIVE", "Referral programme is not active");
        }

        referral.setStatus(ReferralStatus.SIGNED_UP);
        referral.setFraudResultJson(null);
        referral.setUpdatedAt(Instant.now());
        referralRepository.save(referral);

        auditService.log(
            tenantId,
            programme,
            referralUid,
            action,
            ReferralAuditLog.ActorType.ADMIN,
            referral.getRefereeCustomerId(),
            Map.of("note", note != null ? note : "")
        );

        auditService.log(
            tenantId,
            programme,
            referralUid,
            ReferralAuditLog.Action.LINKED,
            ReferralAuditLog.ActorType.ADMIN,
            referral.getRefereeCustomerId(),
            Map.of("referrerCustomerId", referral.getReferrerCustomerId())
        );

        issueSignupRewards(tenantId, programme, active, referral);
    }

    private void issueSignupRewards(
        String tenantId,
        String programmeUid,
        ReferralProgramme programme,
        Referral referral
    ) {
        ReferralProgrammeConfig config = programmeService.readConfig(programme);
        ReferralStageRewards signupRewards = new ReferralStageRewards();
        for (ReferralStageConfig stage : config.getStages()) {
            if (ReferralMilestoneRuleRegistry.isLinkRule(config, stage)) {
                ReferralStageRewards stageRewards = milestoneService.completeLinkStage(
                    tenantId,
                    programmeUid,
                    referral,
                    stage,
                    "review:" + referral.getReferralUid()
                );
                signupRewards.getIssuanceLines().addAll(stageRewards.getIssuanceLines());
                signupRewards.getVoucherGrants().addAll(stageRewards.getVoucherGrants());
            }
        }
        if (!signupRewards.getIssuanceLines().isEmpty() || !signupRewards.getVoucherGrants().isEmpty()) {
            rewardDispatchService.dispatchStageRewards(
                tenantId,
                programmeUid,
                referral.getReferralUid(),
                "review:" + referral.getReferralUid(),
                config,
                signupRewards
            );
            referral.setStatus(ReferralStatus.REWARDED);
            referral.setUpdatedAt(Instant.now());
            referralRepository.save(referral);
        }
    }

    private ReferralFraudQueueItemResponse toQueueItem(Referral referral) {
        ReferralFraudQueueItemResponse item = new ReferralFraudQueueItemResponse();
        item.setReferralUid(referral.getReferralUid());
        item.setReferrerCustomerId(referral.getReferrerCustomerId());
        item.setRefereeCustomerId(referral.getRefereeCustomerId());
        item.setReferralCodeUsed(referral.getReferralCodeUsed());
        item.setCreatedAt(referral.getCreatedAt());
        item.setFraudReasons(parseFraudReasons(referral.getFraudResultJson()));
        return item;
    }

    private List<String> parseFraudReasons(String fraudJson) {
        if (fraudJson == null || fraudJson.isBlank()) {
            return List.of();
        }
        try {
            FraudCheckResult result = objectMapper.readValue(fraudJson, FraudCheckResult.class);
            return result.getReasons() != null ? result.getReasons() : List.of();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }
}
