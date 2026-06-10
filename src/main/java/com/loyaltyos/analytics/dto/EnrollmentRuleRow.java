package com.loyaltyos.analytics.dto;

public class EnrollmentRuleRow {

    private String ruleUid;
    private String ruleName;
    private long newEnrollments;

    public EnrollmentRuleRow() {}

    public EnrollmentRuleRow(String ruleUid, String ruleName, long newEnrollments) {
        this.ruleUid = ruleUid;
        this.ruleName = ruleName;
        this.newEnrollments = newEnrollments;
    }

    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public long getNewEnrollments() { return newEnrollments; }
    public void setNewEnrollments(long newEnrollments) { this.newEnrollments = newEnrollments; }
}
