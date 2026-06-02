package com.loyaltyos.integration.service;

import com.loyaltyos.integration.dto.IntegrationRedemptionRequest;
import com.loyaltyos.integration.dto.IntegrationRedemptionResponse;
import com.loyaltyos.integration.dto.IntegrationRedemptionValidationResponse;
import com.loyaltyos.rewards.dto.RedemptionRequest;
import com.loyaltyos.rewards.dto.RedemptionResult;
import com.loyaltyos.rewards.dto.RedemptionValidationResult;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.service.RewardRedemptionService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IntegrationRedemptionService {

    private final ProgrammeService programmeService;
    private final RewardRedemptionService rewardRedemptionService;

    public IntegrationRedemptionService(
        ProgrammeService programmeService,
        RewardRedemptionService rewardRedemptionService
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.rewardRedemptionService = Objects.requireNonNull(rewardRedemptionService, "rewardRedemptionService");
    }

    public IntegrationRedemptionValidationResponse validate(String tenantId, IntegrationRedemptionRequest request) {
        programmeService.assertProgrammeActiveForIntegration(tenantId, request.getProgrammeUid());
        RedemptionValidationResult core = rewardRedemptionService.validateRedemption(
            tenantId, toCoreRequest(request)
        );
        IntegrationRedemptionValidationResponse out = new IntegrationRedemptionValidationResponse();
        out.setStatus(core.getStatus());
        out.setRedemptionId(core.getRedemptionId());
        out.setTimestamp(core.getTimestamp());
        out.setValid(core.isValid());
        out.setCurrentBalance(core.getCurrentBalance());
        out.setPointsToRedeem(core.getPointsToRedeem());
        out.setFieldErrors(core.getFieldErrors());
        out.setNote(core.getNote());
        out.setCatalogRewardUid(core.getCatalogRewardUid());
        out.setCatalogRewardName(core.getCatalogRewardName());
        out.setCatalogRewardType(core.getCatalogRewardType());
        return out;
    }

    public IntegrationRedemptionResponse redeem(String tenantId, IntegrationRedemptionRequest request) {
        programmeService.assertProgrammeActiveForIntegration(tenantId, request.getProgrammeUid());
        RedemptionResult core = rewardRedemptionService.redeem(tenantId, toCoreRequest(request));
        IntegrationRedemptionResponse out = new IntegrationRedemptionResponse();
        out.setStatus(core.getStatus());
        out.setRedemptionId(core.getRedemptionId());
        out.setCustomerId(core.getCustomerId());
        out.setProgrammeUid(core.getProgrammeUid());
        out.setPointsRedeemed(core.getPointsRedeemed());
        out.setNewBalance(core.getNewBalance());
        out.setLedgerId(core.getLedgerId());
        out.setIdempotentReplay(core.isIdempotentReplay());
        out.setMessage(core.getMessage());
        out.setTimestamp(core.getTimestamp());
        out.setCatalogRewardUid(core.getCatalogRewardUid());
        out.setCatalogRewardName(core.getCatalogRewardName());
        out.setCatalogRewardType(core.getCatalogRewardType());
        return out;
    }

    private static RedemptionRequest toCoreRequest(IntegrationRedemptionRequest request) {
        RedemptionRequest core = new RedemptionRequest();
        core.setRedemptionId(request.getRedemptionId());
        core.setCustomerId(request.getCustomerId());
        core.setProgrammeUid(request.getProgrammeUid());
        core.setPointsToRedeem(request.getPointsToRedeem());
        core.setOrderAmount(request.getOrderAmount());
        core.setCurrency(request.getCurrency());
        core.setChannel(request.getChannel());
        core.setCatalogRewardUid(request.getCatalogRewardUid());
        return core;
    }
}
