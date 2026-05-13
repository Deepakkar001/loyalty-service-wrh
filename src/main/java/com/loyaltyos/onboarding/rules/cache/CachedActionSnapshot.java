package com.loyaltyos.onboarding.rules.cache;

public class CachedActionSnapshot {
    private String actionUid;
    private String actionType;
    private String formula;

    public CachedActionSnapshot() {}

    public CachedActionSnapshot(String actionUid, String actionType, String formula) {
        this.actionUid = actionUid;
        this.actionType = actionType;
        this.formula = formula;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String actionUid;
        private String actionType;
        private String formula;

        private Builder() {}

        public Builder actionUid(String actionUid) { this.actionUid = actionUid; return this; }
        public Builder actionType(String actionType) { this.actionType = actionType; return this; }
        public Builder formula(String formula) { this.formula = formula; return this; }

        public CachedActionSnapshot build() { return new CachedActionSnapshot(actionUid, actionType, formula); }
    }

    public String getActionUid() { return actionUid; }
    public void setActionUid(String actionUid) { this.actionUid = actionUid; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
}
