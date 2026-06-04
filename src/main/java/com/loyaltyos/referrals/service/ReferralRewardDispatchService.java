package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.dto.ReferralIssuanceLine;
import com.loyaltyos.referrals.dto.ReferralStageRewards;
import com.loyaltyos.referrals.dto.ReferralVoucherGrantLine;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import com.loyaltyos.rewards.dto.RewardIssueRequest;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReferralRewardDispatchService {

    private final RewardIssuanceService rewardIssuanceService;
    private final ReferralVoucherRewardService voucherRewardService;

    public ReferralRewardDispatchService(
        RewardIssuanceService rewardIssuanceService,
        ReferralVoucherRewardService voucherRewardService
    ) {
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.voucherRewardService = Objects.requireNonNull(voucherRewardService, "voucherRewardService");
    }

    public BigDecimal dispatchStageRewards(
        String tenantId,
        String programmeUid,
        String referralUid,
        String eventId,
        ReferralProgrammeConfig config,
        ReferralStageRewards rewards
    ) {
        if (rewards == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal points = dispatch(tenantId, programmeUid, eventId, rewards.getIssuanceLines());
        voucherRewardService.dispatch(
            tenantId, programmeUid, referralUid, eventId, config, rewards.getVoucherGrants()
        );
        return points;
    }

    public BigDecimal dispatch(
        String tenantId,
        String programmeUid,
        String eventId,
        List<ReferralIssuanceLine> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<String, List<RewardIssueCommandDto>> byCustomer = new LinkedHashMap<>();
        for (ReferralIssuanceLine line : lines) {
            if (line == null || line.getCustomerId() == null || line.getCommand() == null) {
                continue;
            }
            byCustomer.computeIfAbsent(line.getCustomerId(), k -> new ArrayList<>()).add(line.getCommand());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<RewardIssueCommandDto>> entry : byCustomer.entrySet()) {
            RewardIssueRequest req = new RewardIssueRequest();
            req.setProgrammeUid(programmeUid);
            req.setCustomerId(entry.getKey());
            req.setEventId(eventId);
            req.setRewardCommands(entry.getValue());
            var resp = rewardIssuanceService.issue(tenantId, req);
            if (resp.getTotalPointsIssued() != null) {
                total = total.add(resp.getTotalPointsIssued());
            }
        }
        return total;
    }
}
