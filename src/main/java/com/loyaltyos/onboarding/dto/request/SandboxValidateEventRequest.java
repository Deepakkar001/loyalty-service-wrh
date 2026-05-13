package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Truthful MVP: validate payload against tenant_config.event_schema.
 * The schema format is tenant-defined JSON; for now we just require a JSON payload string.
 */
public class SandboxValidateEventRequest {
    @NotBlank
    private String payloadJson;

    /**
     * Optional: when provided, run evaluation for this specific ruleUid even if it is in DRAFT.
     * Used by the portal "Test (Sandbox)" flow to validate a rule before activation.
     */
    private String ruleUid;

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
}

