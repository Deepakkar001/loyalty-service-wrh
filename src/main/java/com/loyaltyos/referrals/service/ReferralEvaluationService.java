package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.dto.ReferralEvaluationResult;
import com.loyaltyos.referrals.dto.ReferralIssuanceLine;
import com.loyaltyos.referrals.dto.ReferralStageRewards;
import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReferralEvaluationService {

    private final ReferralProgrammeService programmeService;
    private final ReferralMilestoneService milestoneService;
    private final ReferralRepository referralRepository;

    public ReferralEvaluationService(
        ReferralProgrammeService programmeService,
        ReferralMilestoneService milestoneService,
        ReferralRepository referralRepository
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.milestoneService = Objects.requireNonNull(milestoneService, "milestoneService");
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
    }

    public ReferralEvaluationResult evaluateEvent(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        String eventType,
        BigDecimal amount,
        Map<String, Object> eventMetadata
    ) {
        ReferralProgramme programme = programmeService.getActiveOrNull(tenantId, programmeUid);
        if (programme == null) {
            return ReferralEvaluationResult.empty();
        }

        ReferralStageRewards rewards;
        if (eventType != null && "PURCHASE".equalsIgnoreCase(eventType.trim())) {
            rewards = milestoneService.evaluatePurchaseEvent(
                tenantId,
                programmeUid,
                customerId,
                eventId,
                eventType,
                amount,
                eventMetadata
            );
        } else {
            rewards = milestoneService.evaluateIntegrationEvent(
                tenantId,
                programmeUid,
                customerId,
                eventId,
                eventType,
                eventMetadata
            );
        }

        ReferralEvaluationResult result = new ReferralEvaluationResult();
        result.setIssuanceLines(rewards.getIssuanceLines() != null ? rewards.getIssuanceLines() : new ArrayList<>());
        result.setVoucherGrants(rewards.getVoucherGrants());
        result.setReferralPointsAwarded(sumPoints(result.getIssuanceLines()));

        Optional<Referral> referral = referralRepository.findByTenantIdAndProgrammeUidAndRefereeCustomerId(
            tenantId, programmeUid, customerId
        );
        referral.ifPresent(r -> result.setReferralUid(r.getReferralUid()));
        return result;
    }

    private static BigDecimal sumPoints(List<ReferralIssuanceLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ReferralIssuanceLine line : lines) {
            if (line.getCommand() != null && line.getCommand().getPointsToAward() != null) {
                total = total.add(line.getCommand().getPointsToAward());
            }
        }
        return total;
    }
}
