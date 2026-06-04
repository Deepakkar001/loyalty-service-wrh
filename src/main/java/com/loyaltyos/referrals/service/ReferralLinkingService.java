package com.loyaltyos.referrals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.dto.ReferralStageRewards;
import com.loyaltyos.referrals.dto.ReferralLinkResponse;
import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.entity.ReferralAuditLog;
import com.loyaltyos.referrals.entity.ReferralCode;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.enums.ReferralStatus;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralLinkSignals;
import com.loyaltyos.referrals.support.ReferralMilestoneRuleRegistry;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.referrals.service.ReferralFraudService.FraudCheckResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralLinkingService {

    private final ReferralProgrammeService programmeService;
    private final ReferralCodeService codeService;
    private final ReferralRepository referralRepository;
    private final ReferralFraudService fraudService;
    private final ReferralEligibilityService eligibilityService;
    private final ReferralCapService capService;
    private final ReferralAuditService auditService;
    private final ReferralMilestoneService milestoneService;
    private final ReferralRewardDispatchService rewardDispatchService;
    private final ObjectMapper objectMapper;

    public ReferralLinkingService(
        ReferralProgrammeService programmeService,
        ReferralCodeService codeService,
        ReferralRepository referralRepository,
        ReferralFraudService fraudService,
        ReferralEligibilityService eligibilityService,
        ReferralCapService capService,
        ReferralAuditService auditService,
        ReferralMilestoneService milestoneService,
        ReferralRewardDispatchService rewardDispatchService,
        ObjectMapper objectMapper
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.codeService = Objects.requireNonNull(codeService, "codeService");
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
        this.fraudService = Objects.requireNonNull(fraudService, "fraudService");
        this.eligibilityService = Objects.requireNonNull(eligibilityService, "eligibilityService");
        this.capService = Objects.requireNonNull(capService, "capService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.milestoneService = Objects.requireNonNull(milestoneService, "milestoneService");
        this.rewardDispatchService = Objects.requireNonNull(rewardDispatchService, "rewardDispatchService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional
    public ReferralLinkResponse linkReferee(
        String tenantId,
        String programmeUid,
        String referralCode,
        String refereeCustomerId,
        ReferralLinkSignals referrerSignals,
        ReferralLinkSignals refereeSignals
    ) {
        String programme = normalizeProgramme(programmeUid);
        ReferralProgramme active = programmeService.getActiveOrNull(tenantId, programme);
        if (active == null) {
            throw new ReferralException("REFERRAL_PROGRAMME_INACTIVE", "Referral programme is not active");
        }

        ReferralProgrammeConfig programmeConfig = programmeService.readConfig(active);

        Optional<Referral> existing = referralRepository.findByTenantIdAndProgrammeUidAndRefereeCustomerId(
            tenantId, programme, refereeCustomerId
        );
        if (existing.isPresent()) {
            return toLinkResponse(existing.get(), true);
        }

        ReferralCode code = codeService.lookupActiveCode(tenantId, referralCode);
        String referrerId = code.getCustomerId();

        eligibilityService.assertReferrerEligible(tenantId, programme, referrerId, programmeConfig);
        eligibilityService.assertRefereeEligible(tenantId, programme, refereeCustomerId, programmeConfig);
        capService.assertReferrerWithinCaps(tenantId, programme, referrerId, active, programmeConfig);

        FraudCheckResult fraud = fraudService.check(
            tenantId, programme, referrerId, refereeCustomerId, programmeConfig, referrerSignals, refereeSignals
        );

        Referral referral = new Referral();
        referral.setTenantId(tenantId);
        referral.setProgrammeUid(programme);
        referral.setReferralUid("ref_" + UUID.randomUUID());
        referral.setReferrerCustomerId(referrerId);
        referral.setRefereeCustomerId(refereeCustomerId);
        referral.setReferralCodeUsed(code.getCode());
        referral.setCreatedAt(Instant.now());
        referral.setUpdatedAt(Instant.now());

        if (!fraud.isPassed()) {
            referral.setStatus(ReferralStatus.FRAUD_FLAGGED);
            try {
                referral.setFraudResultJson(objectMapper.writeValueAsString(fraud));
            } catch (Exception ignored) {
                referral.setFraudResultJson("{}");
            }
            referral.setUpdatedAt(Instant.now());
            try {
                referralRepository.save(referral);
            } catch (DataIntegrityViolationException ex) {
                return toLinkResponse(
                    referralRepository.findByTenantIdAndProgrammeUidAndRefereeCustomerId(
                        tenantId, programme, refereeCustomerId
                    ).orElseThrow(() -> ex),
                    true
                );
            }
            auditService.log(
                tenantId,
                programme,
                referral.getReferralUid(),
                ReferralAuditLog.Action.FRAUD_FLAGGED,
                ReferralAuditLog.ActorType.SYSTEM,
                refereeCustomerId,
                Map.of("reasons", fraud.getReasons())
            );
            throw new ReferralException("REFERRAL_FRAUD", "Referral flagged: " + String.join(",", fraud.getReasons()));
        }

        referral.setStatus(ReferralStatus.SIGNED_UP);
        try {
            referralRepository.save(referral);
        } catch (DataIntegrityViolationException ex) {
            Referral replay = referralRepository
                .findByTenantIdAndProgrammeUidAndRefereeCustomerId(tenantId, programme, refereeCustomerId)
                .orElseThrow(() -> ex);
            return toLinkResponse(replay, true);
        }

        auditService.log(
            tenantId,
            programme,
            referral.getReferralUid(),
            ReferralAuditLog.Action.LINKED,
            ReferralAuditLog.ActorType.SYSTEM,
            refereeCustomerId,
            Map.of("referrerCustomerId", referrerId, "code", code.getCode())
        );

        ReferralProgrammeConfig config = programmeService.readConfig(active);
        ReferralStageRewards signupRewards = new ReferralStageRewards();
        for (ReferralStageConfig stage : config.getStages()) {
            if (ReferralMilestoneRuleRegistry.isLinkRule(config, stage)) {
                ReferralStageRewards stageRewards = milestoneService.completeLinkStage(
                    tenantId, programme, referral, stage, "link:" + refereeCustomerId
                );
                signupRewards.getIssuanceLines().addAll(stageRewards.getIssuanceLines());
                signupRewards.getVoucherGrants().addAll(stageRewards.getVoucherGrants());
            }
        }
        if (!signupRewards.getIssuanceLines().isEmpty() || !signupRewards.getVoucherGrants().isEmpty()) {
            rewardDispatchService.dispatchStageRewards(
                tenantId,
                programme,
                referral.getReferralUid(),
                "link:" + refereeCustomerId,
                config,
                signupRewards
            );
            referral.setStatus(ReferralStatus.REWARDED);
            referralRepository.save(referral);
        }

        return toLinkResponse(referral, false);
    }

    private static ReferralLinkResponse toLinkResponse(Referral referral, boolean idempotentReplay) {
        ReferralLinkResponse r = new ReferralLinkResponse();
        r.setReferralUid(referral.getReferralUid());
        r.setReferrerCustomerId(referral.getReferrerCustomerId());
        r.setRefereeCustomerId(referral.getRefereeCustomerId());
        r.setStatus(referral.getStatus().name());
        r.setIdempotentReplay(idempotentReplay);
        return r;
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }
}
