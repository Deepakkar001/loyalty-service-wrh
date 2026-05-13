package com.loyaltyos.onboarding.rules.cache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class CachedRuleSnapshot {
    private long ruleDbId;
    private String ruleUid;
    private String name;
    private int priority;
    private String executionMode;
    private JsonNode conditionTree;
    private List<CachedActionSnapshot> actions = new ArrayList<>();

    public CachedRuleSnapshot() {}

    public CachedRuleSnapshot(
        long ruleDbId,
        String ruleUid,
        String name,
        int priority,
        String executionMode,
        JsonNode conditionTree,
        List<CachedActionSnapshot> actions
    ) {
        this.ruleDbId = ruleDbId;
        this.ruleUid = ruleUid;
        this.name = name;
        this.priority = priority;
        this.executionMode = executionMode;
        this.conditionTree = conditionTree;
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private long ruleDbId;
        private String ruleUid;
        private String name;
        private int priority;
        private String executionMode;
        private JsonNode conditionTree;
        private List<CachedActionSnapshot> actions = new ArrayList<>();

        private Builder() {}

        public Builder ruleDbId(long ruleDbId) { this.ruleDbId = ruleDbId; return this; }
        public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder executionMode(String executionMode) { this.executionMode = executionMode; return this; }
        public Builder conditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; return this; }
        public Builder actions(List<CachedActionSnapshot> actions) { this.actions = actions != null ? actions : new ArrayList<>(); return this; }

        public CachedRuleSnapshot build() {
            return new CachedRuleSnapshot(ruleDbId, ruleUid, name, priority, executionMode, conditionTree, actions);
        }
    }

    public long getRuleDbId() { return ruleDbId; }
    public void setRuleDbId(long ruleDbId) { this.ruleDbId = ruleDbId; }
    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public JsonNode getConditionTree() { return conditionTree; }
    public void setConditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; }
    public List<CachedActionSnapshot> getActions() { return actions; }
    public void setActions(List<CachedActionSnapshot> actions) { this.actions = actions != null ? actions : new ArrayList<>(); }
}
