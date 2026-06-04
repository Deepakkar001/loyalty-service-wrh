package com.loyaltyos.referrals.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

public class ReferralStageConfig {

    private int stage;
    /** Milestone rule key ({@link ReferralMilestoneRule#getKey()}). */
    private String type;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, Object> condition;
    private BigDecimal referrerPoints = BigDecimal.ZERO;
    private BigDecimal refereePoints = BigDecimal.ZERO;
    /** Max times a referrer can earn this stage (across different referees). Null = unlimited. */
    private Integer maxReferrerAwardsForStage;
    private ReferralPartyRewardConfig referrerReward;
    private ReferralPartyRewardConfig refereeReward;

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getCondition() {
        return condition;
    }

    public void setCondition(Map<String, Object> condition) {
        this.condition = condition;
    }

    public BigDecimal getReferrerPoints() {
        return referrerPoints;
    }

    public void setReferrerPoints(BigDecimal referrerPoints) {
        this.referrerPoints = referrerPoints;
    }

    public BigDecimal getRefereePoints() {
        return refereePoints;
    }

    public void setRefereePoints(BigDecimal refereePoints) {
        this.refereePoints = refereePoints;
    }

    public Integer getMaxReferrerAwardsForStage() {
        return maxReferrerAwardsForStage;
    }

    public void setMaxReferrerAwardsForStage(Integer maxReferrerAwardsForStage) {
        this.maxReferrerAwardsForStage = maxReferrerAwardsForStage;
    }

    public ReferralPartyRewardConfig getReferrerReward() {
        return referrerReward;
    }

    public void setReferrerReward(ReferralPartyRewardConfig referrerReward) {
        this.referrerReward = referrerReward;
    }

    public ReferralPartyRewardConfig getRefereeReward() {
        return refereeReward;
    }

    public void setRefereeReward(ReferralPartyRewardConfig refereeReward) {
        this.refereeReward = refereeReward;
    }
}
