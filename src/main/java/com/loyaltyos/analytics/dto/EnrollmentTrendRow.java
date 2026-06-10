package com.loyaltyos.analytics.dto;

public class EnrollmentTrendRow {

    private String period;
    private long newEnrollments;

    public EnrollmentTrendRow() {}

    public EnrollmentTrendRow(String period, long newEnrollments) {
        this.period = period;
        this.newEnrollments = newEnrollments;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public long getNewEnrollments() { return newEnrollments; }
    public void setNewEnrollments(long newEnrollments) { this.newEnrollments = newEnrollments; }
}
