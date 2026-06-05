package com.loyaltyos.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sandbox event validation: parses {@code payloadJson} and validates required fields against the
 * active {@code programme_config} JSON for the {@code programmeUid} in the payload (default programme when omitted).
 */
public class SandboxValidateEventRequest {
    @NotBlank
    private String payloadJson;

    /**
     * Optional: when provided, run evaluation for this specific ruleUid even if it is in DRAFT.
     * Used by the portal "Test (Sandbox)" flow to validate a rule before activation.
     */
    private String ruleUid;

    /** Optional explicit campaign; otherwise derived from CAMPAIGN rule when ruleUid is set. */
    private String campaignUid;

    public SandboxValidateEventRequest() {}

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getRuleUid() {
        return ruleUid;
    }

    public void setRuleUid(String ruleUid) {
        this.ruleUid = ruleUid;
    }

    public String getCampaignUid() {
        return campaignUid;
    }

    public void setCampaignUid(String campaignUid) {
        this.campaignUid = campaignUid;
    }
}

