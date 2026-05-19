package com.loyaltyos.onboarding.rules.dto;

import com.loyaltyos.onboarding.rules.enums.RuleStatus;

public class EarnRuleResponse {
    private Long id;
    private String tenantId;
    private String programmeUid;
    private String ruleType;
    private String campaignUid;
    private String ruleUid;
    private String name;
    private RuleStatus status;
    private String triggerEventType;
    private String executionMode;

    public EarnRuleResponse() {}

    public EarnRuleResponse(
        Long id,
        String tenantId,
        String programmeUid,
        String ruleType,
        String campaignUid,
        String ruleUid,
        String name,
        RuleStatus status,
        String triggerEventType,
        String executionMode
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.ruleType = ruleType;
        this.campaignUid = campaignUid;
        this.ruleUid = ruleUid;
        this.name = name;
        this.status = status;
        this.triggerEventType = triggerEventType;
        this.executionMode = executionMode;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid;
        private String ruleType;
        private String campaignUid;
        private String ruleUid;
        private String name;
        private RuleStatus status;
        private String triggerEventType;
        private String executionMode;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder ruleType(String ruleType) { this.ruleType = ruleType; return this; }
        public Builder campaignUid(String campaignUid) { this.campaignUid = campaignUid; return this; }
        public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder status(RuleStatus status) { this.status = status; return this; }
        public Builder triggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; return this; }
        public Builder executionMode(String executionMode) { this.executionMode = executionMode; return this; }

        public EarnRuleResponse build() {
            return new EarnRuleResponse(
                id, tenantId, programmeUid, ruleType, campaignUid, ruleUid, name, status, triggerEventType, executionMode
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RuleStatus getStatus() { return status; }
    public void setStatus(RuleStatus status) { this.status = status; }
    public String getTriggerEventType() { return triggerEventType; }
    public void setTriggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
}
