package com.loyaltyos.onboarding.rules.dto;

import com.loyaltyos.onboarding.rules.enums.RuleChangeType;

import java.time.Instant;

public class RuleChangeLogResponse {
    private Long id;
    private String ruleUid;
    private RuleChangeType changeType;
    private String changedBy;
    private String beforeState;
    private String afterState;
    private Instant changedAt;

    public RuleChangeLogResponse() {}

    public RuleChangeLogResponse(
        Long id,
        String ruleUid,
        RuleChangeType changeType,
        String changedBy,
        String beforeState,
        String afterState,
        Instant changedAt
    ) {
        this.id = id;
        this.ruleUid = ruleUid;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.changedAt = changedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String ruleUid;
        private RuleChangeType changeType;
        private String changedBy;
        private String beforeState;
        private String afterState;
        private Instant changedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
        public Builder changeType(RuleChangeType changeType) { this.changeType = changeType; return this; }
        public Builder changedBy(String changedBy) { this.changedBy = changedBy; return this; }
        public Builder beforeState(String beforeState) { this.beforeState = beforeState; return this; }
        public Builder afterState(String afterState) { this.afterState = afterState; return this; }
        public Builder changedAt(Instant changedAt) { this.changedAt = changedAt; return this; }

        public RuleChangeLogResponse build() {
            return new RuleChangeLogResponse(id, ruleUid, changeType, changedBy, beforeState, afterState, changedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public RuleChangeType getChangeType() { return changeType; }
    public void setChangeType(RuleChangeType changeType) { this.changeType = changeType; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }
    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}

