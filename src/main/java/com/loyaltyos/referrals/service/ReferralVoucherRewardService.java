package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.dto.ReferralVoucherGrantLine;
import com.loyaltyos.referrals.entity.ReferralAuditLog;
import com.loyaltyos.referrals.entity.ReferralRewardIssued;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralPartyRewardConfig;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralRewardType;
import com.loyaltyos.referrals.repository.ReferralRewardIssuedRepository;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import com.loyaltyos.rewards.dto.RewardIssueRequest;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import com.loyaltyos.voucher.service.VoucherIssueService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReferralVoucherRewardService {

    private final ReferralRewardIssuedRepository rewardsIssuedRepository;
    private final ReferralBudgetService budgetService;
    private final ReferralAuditService auditService;
    private final RewardIssuanceService rewardIssuanceService;
    private final VoucherIssueService voucherIssueService;

    public ReferralVoucherRewardService(
        ReferralRewardIssuedRepository rewardsIssuedRepository,
        ReferralBudgetService budgetService,
        ReferralAuditService auditService,
        RewardIssuanceService rewardIssuanceService,
        VoucherIssueService voucherIssueService
    ) {
        this.rewardsIssuedRepository = Objects.requireNonNull(rewardsIssuedRepository, "rewardsIssuedRepository");
        this.budgetService = Objects.requireNonNull(budgetService, "budgetService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.voucherIssueService = Objects.requireNonNull(voucherIssueService, "voucherIssueService");
    }

    public void dispatch(
        String tenantId,
        String programmeUid,
        String referralUid,
        String eventId,
        ReferralProgrammeConfig config,
        List<ReferralVoucherGrantLine> grants
    ) {
        if (grants == null || grants.isEmpty()) {
            return;
        }
        for (ReferralVoucherGrantLine grant : grants) {
            if (grant == null || grant.getCustomerId() == null || grant.getCatalogRewardUid() == null) {
                continue;
            }
            issueOne(tenantId, programmeUid, referralUid, eventId, config, grant);
        }
    }

    private void issueOne(
        String tenantId,
        String programmeUid,
        String referralUid,
        String eventId,
        ReferralProgrammeConfig config,
        ReferralVoucherGrantLine grant
    ) {
        String redemptionId = grant.getRedemptionId();
        if (redemptionId == null || redemptionId.isBlank()) {
            throw new ReferralException("INVALID_VOUCHER_GRANT", "Voucher grant missing redemption id");
        }

        if (rewardsIssuedRepository.findByTenantIdAndRecipientCustomerIdAndIdempotencyKey(
            tenantId, grant.getCustomerId(), redemptionId
        ).isPresent()) {
            return;
        }

        BigDecimal funding = grant.getFundingPoints() != null ? grant.getFundingPoints() : BigDecimal.ZERO;
        if (funding.signum() > 0) {
            budgetService.assertWithinBudget(tenantId, programmeUid, config, funding);
            String fundKey = redemptionId + ":fund";
            if (rewardsIssuedRepository.findByTenantIdAndRecipientCustomerIdAndIdempotencyKey(
                tenantId, grant.getCustomerId(), fundKey
            ).isEmpty()) {
                RewardIssueCommandDto cmd = new RewardIssueCommandDto();
                cmd.setIdempotencyKey(fundKey);
                cmd.setPointsToAward(funding.setScale(4, RoundingMode.HALF_UP));
                cmd.setActionType("AWARD_POINTS");
                cmd.setSourceRuleUid("referral:" + referralUid);
                RewardIssueRequest req = new RewardIssueRequest();
                req.setProgrammeUid(programmeUid);
                req.setCustomerId(grant.getCustomerId());
                req.setEventId(eventId);
                req.setRewardCommands(List.of(cmd));
                rewardIssuanceService.issue(tenantId, req);
            }
        }

        VoucherIssueResponse voucher = voucherIssueService.issueVoucher(
            tenantId,
            programmeUid,
            grant.getCatalogRewardUid(),
            grant.getCustomerId(),
            redemptionId,
            grant.getPointsToRedeem(),
            grant.getFaceValue()
        );

        ReferralRewardIssued issued = new ReferralRewardIssued();
        issued.setTenantId(tenantId);
        issued.setProgrammeUid(programmeUid);
        issued.setReferralUid(referralUid);
        issued.setStage(grant.getStage());
        issued.setRecipientType(ReferralRewardIssued.RecipientType.valueOf(grant.getRecipientType()));
        issued.setRewardType(ReferralRewardType.VOUCHER.name());
        issued.setCatalogRewardUid(grant.getCatalogRewardUid());
        issued.setRecipientCustomerId(grant.getCustomerId());
        BigDecimal recorded = funding.signum() > 0
            ? funding
            : (grant.getPointsToRedeem() != null ? grant.getPointsToRedeem() : BigDecimal.ZERO);
        issued.setPointsAwarded(recorded.setScale(4, RoundingMode.HALF_UP));
        issued.setIdempotencyKey(redemptionId);
        issued.setCreatedAt(Instant.now());
        rewardsIssuedRepository.save(issued);

        auditService.log(
            tenantId,
            programmeUid,
            referralUid,
            ReferralAuditLog.Action.REWARD_ISSUED,
            ReferralAuditLog.ActorType.SYSTEM,
            grant.getCustomerId(),
            Map.of(
                "stage", grant.getStage(),
                "rewardType", "VOUCHER",
                "catalogRewardUid", grant.getCatalogRewardUid(),
                "eventId", eventId,
                "voucherCode",
                voucher.getVoucher() != null && voucher.getVoucher().getCode() != null
                    ? voucher.getVoucher().getCode()
                    : ""
            )
        );
    }

    public static BigDecimal resolveFundingPoints(ReferralPartyRewardConfig reward) {
        if (reward == null || reward.getType() != ReferralRewardType.VOUCHER) {
            return BigDecimal.ZERO;
        }
        if (reward.getVoucherPointsToRedeem() != null && reward.getVoucherPointsToRedeem().signum() > 0) {
            return reward.getVoucherPointsToRedeem();
        }
        return BigDecimal.ZERO;
    }
}
