package com.loyaltyos.referrals.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ReferralProgrammeConfig {

    private List<ReferralStageConfig> stages = new ArrayList<>();
    private ReferralEligibilityRules eligibility = new ReferralEligibilityRules();
    private List<ReferralCapRule> capRules = new ArrayList<>();
    private ReferralFraudPolicy fraudPolicy = new ReferralFraudPolicy();
    private ReferralPointsBudget pointsBudget;
    /** Tenant-defined milestone rules; stages reference {@link ReferralMilestoneRule#getKey()}. */
    private List<ReferralMilestoneRule> milestoneRules = new ArrayList<>();

    /** Legacy: read-only for migration from old JSON. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<String> enabledMilestoneTypes;

    /** Legacy: read-only for migration from old JSON. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ReferralLegacyConfigPayload> customMilestoneTypes;

    public List<ReferralStageConfig> getStages() {
        return stages;
    }

    public void setStages(List<ReferralStageConfig> stages) {
        this.stages = stages != null ? stages : new ArrayList<>();
    }

    public ReferralEligibilityRules getEligibility() {
        return eligibility;
    }

    public void setEligibility(ReferralEligibilityRules eligibility) {
        this.eligibility = eligibility != null ? eligibility : new ReferralEligibilityRules();
    }

    public List<ReferralCapRule> getCapRules() {
        return capRules;
    }

    public void setCapRules(List<ReferralCapRule> capRules) {
        this.capRules = capRules != null ? capRules : new ArrayList<>();
    }

    public ReferralFraudPolicy getFraudPolicy() {
        return fraudPolicy;
    }

    public void setFraudPolicy(ReferralFraudPolicy fraudPolicy) {
        this.fraudPolicy = fraudPolicy != null ? fraudPolicy : new ReferralFraudPolicy();
    }

    public ReferralPointsBudget getPointsBudget() {
        return pointsBudget;
    }

    public void setPointsBudget(ReferralPointsBudget pointsBudget) {
        this.pointsBudget = pointsBudget;
    }

    public List<String> getEnabledMilestoneTypes() {
        return enabledMilestoneTypes;
    }

    public void setEnabledMilestoneTypes(List<String> enabledMilestoneTypes) {
        this.enabledMilestoneTypes = enabledMilestoneTypes;
    }

    public List<ReferralLegacyConfigPayload> getCustomMilestoneTypes() {
        return customMilestoneTypes;
    }

    public void setCustomMilestoneTypes(List<ReferralLegacyConfigPayload> customMilestoneTypes) {
        this.customMilestoneTypes = customMilestoneTypes;
    }

    public List<ReferralMilestoneRule> getMilestoneRules() {
        return milestoneRules;
    }

    public void setMilestoneRules(List<ReferralMilestoneRule> milestoneRules) {
        this.milestoneRules = milestoneRules != null ? milestoneRules : new ArrayList<>();
    }
}
