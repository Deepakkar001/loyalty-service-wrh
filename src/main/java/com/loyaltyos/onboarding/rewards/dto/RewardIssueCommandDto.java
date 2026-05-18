package com.loyaltyos.onboarding.rewards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One earn line from {@link com.loyaltyos.onboarding.rules.dto.RuleEvaluationResponse.RewardCommand}.
 */
public class RewardIssueCommandDto {

    @NotBlank
    @Size(max = 128)
    /** Unique per {@code (tenant, customer)} row in {@code points_ledger}; stable across HTTP retries for the same earn line. */
    private String idempotencyKey;

    @Size(max = 128)
    private String sourceRuleUid;

    /** Campaign credit: set to {@code campaign_uid}; ledger {@code source_campaign_id}. */
    @Size(max = 128)
    private String sourceCampaignUid;

    private Instant expiresAt;

    private String actionType = "AWARD_POINTS";

    @NotNull
    @Positive
    private BigDecimal pointsToAward;

    /** Optional echo from rule engine. */
    private String commandId;

    public RewardIssueCommandDto() {}

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSourceRuleUid() {
        return sourceRuleUid;
    }

    public void setSourceRuleUid(String sourceRuleUid) {
        this.sourceRuleUid = sourceRuleUid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public BigDecimal getPointsToAward() {
        return pointsToAward;
    }

    public void setPointsToAward(BigDecimal pointsToAward) {
        this.pointsToAward = pointsToAward;
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getSourceCampaignUid() {
        return sourceCampaignUid;
    }

    public void setSourceCampaignUid(String sourceCampaignUid) {
        this.sourceCampaignUid = sourceCampaignUid;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
