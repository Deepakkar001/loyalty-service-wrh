package com.loyaltyos.analytics.dto;

public class EnrollmentSourceRow {

    private String sourceType;
    private long newEnrollments;

    public EnrollmentSourceRow() {}

    public EnrollmentSourceRow(String sourceType, long newEnrollments) {
        this.sourceType = sourceType;
        this.newEnrollments = newEnrollments;
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public long getNewEnrollments() { return newEnrollments; }
    public void setNewEnrollments(long newEnrollments) { this.newEnrollments = newEnrollments; }
}
