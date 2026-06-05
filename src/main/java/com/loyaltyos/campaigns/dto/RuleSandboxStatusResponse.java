package com.loyaltyos.campaigns.dto;

import java.time.Instant;

public class RuleSandboxStatusResponse {

    private String tenantId;
    private String ruleUid;
    private String campaignUid;
    private boolean sandboxPassed;
    private boolean targetedCustomerOk;
    private String customerId;
    private Instant passedAt;

    public static RuleSandboxStatusResponse notPassed(String tenantId, String ruleUid) {
        RuleSandboxStatusResponse r = new RuleSandboxStatusResponse();
        r.tenantId = tenantId;
        r.ruleUid = ruleUid;
        r.sandboxPassed = false;
        r.targetedCustomerOk = false;
        return r;
    }

    public static RuleSandboxStatusResponse passed(
        String tenantId,
        String ruleUid,
        String campaignUid,
        String customerId,
        boolean targetedCustomerOk,
        Instant passedAt
    ) {
        RuleSandboxStatusResponse r = new RuleSandboxStatusResponse();
        r.tenantId = tenantId;
        r.ruleUid = ruleUid;
        r.campaignUid = campaignUid;
        r.sandboxPassed = true;
        r.targetedCustomerOk = targetedCustomerOk;
        r.customerId = customerId;
        r.passedAt = passedAt;
        return r;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public boolean isSandboxPassed() { return sandboxPassed; }
    public void setSandboxPassed(boolean sandboxPassed) { this.sandboxPassed = sandboxPassed; }
    public boolean isTargetedCustomerOk() { return targetedCustomerOk; }
    public void setTargetedCustomerOk(boolean targetedCustomerOk) { this.targetedCustomerOk = targetedCustomerOk; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Instant getPassedAt() { return passedAt; }
    public void setPassedAt(Instant passedAt) { this.passedAt = passedAt; }
}
