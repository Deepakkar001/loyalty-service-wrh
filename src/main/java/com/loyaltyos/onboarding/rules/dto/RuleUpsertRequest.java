package com.loyaltyos.onboarding.rules.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.onboarding.rules.enums.RuleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class RuleUpsertRequest {

    private String programmeUid;

    /** PROGRAMME (default) or CAMPAIGN. */
    private String ruleType;

    /** Required when ruleType is CAMPAIGN. */
    private String campaignUid;

    /** Optional; generated when absent on create. */
    private String ruleUid;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Integer priority;

    @NotBlank
    private String triggerEventType;

    @NotBlank
    private String executionMode;

    @NotNull
    private RuleStatus status;

    @NotNull
    private JsonNode conditionTree;

    /** Optional window; null means open-ended / unchanged semantics per API. */
    private Instant effectiveAt;

    private Instant endAt;

    @Valid
    @NotEmpty
    private List<RuleActionUpsertItem> actions;

    public static class RuleActionUpsertItem {
        private String actionUid;
        @NotBlank
        private String actionType;
        private String formula;
        private JsonNode config;

        public RuleActionUpsertItem() {}

        public String getActionUid() { return actionUid; }
        public void setActionUid(String actionUid) { this.actionUid = actionUid; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        public JsonNode getConfig() { return config; }
        public void setConfig(JsonNode config) { this.config = config; }
    }

    public RuleUpsertRequest() {}

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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getTriggerEventType() { return triggerEventType; }
    public void setTriggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public RuleStatus getStatus() { return status; }
    public void setStatus(RuleStatus status) { this.status = status; }
    public JsonNode getConditionTree() { return conditionTree; }
    public void setConditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public List<RuleActionUpsertItem> getActions() { return actions; }
    public void setActions(List<RuleActionUpsertItem> actions) { this.actions = actions; }
}
