package com.loyaltyos.onboarding.rules.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.onboarding.rules.enums.RuleStatus;

import java.time.Instant;
import java.util.List;

public class EarnRuleDetailResponse {
    private Long id;
    private String tenantId;
    private String programmeUid;
    private String ruleType;
    private String campaignUid;
    private String ruleUid;
    private String name;
    private String description;
    private Integer priority;
    private RuleStatus status;
    private String triggerEventType;
    private String executionMode;
    private Instant effectiveAt;
    private Instant endAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant activatedAt;
    private Instant archivedAt;
    private JsonNode conditionTree;
    private List<ActionItem> actions;

    public static class ActionItem {
        private Long id;
        private String actionUid;
        private String actionType;
        private String formula;
        private JsonNode config;

        public ActionItem() {}

        public ActionItem(Long id, String actionUid, String actionType, String formula, JsonNode config) {
            this.id = id;
            this.actionUid = actionUid;
            this.actionType = actionType;
            this.formula = formula;
            this.config = config;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private Long id;
            private String actionUid;
            private String actionType;
            private String formula;
            private JsonNode config;

            private Builder() {}

            public Builder id(Long id) { this.id = id; return this; }
            public Builder actionUid(String actionUid) { this.actionUid = actionUid; return this; }
            public Builder actionType(String actionType) { this.actionType = actionType; return this; }
            public Builder formula(String formula) { this.formula = formula; return this; }
            public Builder config(JsonNode config) { this.config = config; return this; }

            public ActionItem build() { return new ActionItem(id, actionUid, actionType, formula, config); }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getActionUid() { return actionUid; }
        public void setActionUid(String actionUid) { this.actionUid = actionUid; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        public JsonNode getConfig() { return config; }
        public void setConfig(JsonNode config) { this.config = config; }
    }

    public EarnRuleDetailResponse() {}

    public EarnRuleDetailResponse(
        Long id,
        String tenantId,
        String programmeUid,
        String ruleType,
        String campaignUid,
        String ruleUid,
        String name,
        String description,
        Integer priority,
        RuleStatus status,
        String triggerEventType,
        String executionMode,
        Instant effectiveAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant archivedAt,
        JsonNode conditionTree,
        List<ActionItem> actions
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.ruleType = ruleType;
        this.campaignUid = campaignUid;
        this.ruleUid = ruleUid;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.triggerEventType = triggerEventType;
        this.executionMode = executionMode;
        this.effectiveAt = effectiveAt;
        this.endAt = endAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.activatedAt = activatedAt;
        this.archivedAt = archivedAt;
        this.conditionTree = conditionTree;
        this.actions = actions;
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
        private String description;
        private Integer priority;
        private RuleStatus status;
        private String triggerEventType;
        private String executionMode;
        private Instant effectiveAt;
        private Instant endAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant activatedAt;
        private Instant archivedAt;
        private JsonNode conditionTree;
        private List<ActionItem> actions;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder ruleType(String ruleType) { this.ruleType = ruleType; return this; }
        public Builder campaignUid(String campaignUid) { this.campaignUid = campaignUid; return this; }
        public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder status(RuleStatus status) { this.status = status; return this; }
        public Builder triggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; return this; }
        public Builder executionMode(String executionMode) { this.executionMode = executionMode; return this; }
        public Builder effectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; return this; }
        public Builder endAt(Instant endAt) { this.endAt = endAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder archivedAt(Instant archivedAt) { this.archivedAt = archivedAt; return this; }
        public Builder conditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; return this; }
        public Builder actions(List<ActionItem> actions) { this.actions = actions; return this; }

        public EarnRuleDetailResponse build() {
            return new EarnRuleDetailResponse(
                id, tenantId, programmeUid, ruleType, campaignUid, ruleUid, name, description, priority, status,
                triggerEventType, executionMode, effectiveAt, endAt, createdAt, updatedAt,
                activatedAt, archivedAt, conditionTree, actions
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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public RuleStatus getStatus() { return status; }
    public void setStatus(RuleStatus status) { this.status = status; }
    public String getTriggerEventType() { return triggerEventType; }
    public void setTriggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public JsonNode getConditionTree() { return conditionTree; }
    public void setConditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; }
    public List<ActionItem> getActions() { return actions; }
    public void setActions(List<ActionItem> actions) { this.actions = actions; }
}

