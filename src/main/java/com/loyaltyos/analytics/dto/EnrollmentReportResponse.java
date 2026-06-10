package com.loyaltyos.analytics.dto;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String enrollmentDefinition;

    private long newEnrollmentsInPeriod;
    private long newEnrollmentsPriorPeriod;
    private long totalEnrolledMembers;
    private long returningActiveInPeriod;
    private long newEnrollmentsYtd;
    private Double periodOverPeriodChangePct;

    private List<EnrollmentTrendRow> dailyNewEnrollments = new ArrayList<>();
    private List<EnrollmentTrendRow> monthlyNewEnrollments = new ArrayList<>();
    private List<EnrollmentSourceRow> enrollmentsBySource = new ArrayList<>();
    private List<EnrollmentRuleRow> topEnrollmentRules = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getEnrollmentDefinition() { return enrollmentDefinition; }
    public void setEnrollmentDefinition(String enrollmentDefinition) { this.enrollmentDefinition = enrollmentDefinition; }
    public long getNewEnrollmentsInPeriod() { return newEnrollmentsInPeriod; }
    public void setNewEnrollmentsInPeriod(long newEnrollmentsInPeriod) { this.newEnrollmentsInPeriod = newEnrollmentsInPeriod; }
    public long getNewEnrollmentsPriorPeriod() { return newEnrollmentsPriorPeriod; }
    public void setNewEnrollmentsPriorPeriod(long newEnrollmentsPriorPeriod) { this.newEnrollmentsPriorPeriod = newEnrollmentsPriorPeriod; }
    public long getTotalEnrolledMembers() { return totalEnrolledMembers; }
    public void setTotalEnrolledMembers(long totalEnrolledMembers) { this.totalEnrolledMembers = totalEnrolledMembers; }
    public long getReturningActiveInPeriod() { return returningActiveInPeriod; }
    public void setReturningActiveInPeriod(long returningActiveInPeriod) { this.returningActiveInPeriod = returningActiveInPeriod; }
    public long getNewEnrollmentsYtd() { return newEnrollmentsYtd; }
    public void setNewEnrollmentsYtd(long newEnrollmentsYtd) { this.newEnrollmentsYtd = newEnrollmentsYtd; }
    public Double getPeriodOverPeriodChangePct() { return periodOverPeriodChangePct; }
    public void setPeriodOverPeriodChangePct(Double periodOverPeriodChangePct) { this.periodOverPeriodChangePct = periodOverPeriodChangePct; }
    public List<EnrollmentTrendRow> getDailyNewEnrollments() { return dailyNewEnrollments; }
    public void setDailyNewEnrollments(List<EnrollmentTrendRow> dailyNewEnrollments) { this.dailyNewEnrollments = dailyNewEnrollments; }
    public List<EnrollmentTrendRow> getMonthlyNewEnrollments() { return monthlyNewEnrollments; }
    public void setMonthlyNewEnrollments(List<EnrollmentTrendRow> monthlyNewEnrollments) { this.monthlyNewEnrollments = monthlyNewEnrollments; }
    public List<EnrollmentSourceRow> getEnrollmentsBySource() { return enrollmentsBySource; }
    public void setEnrollmentsBySource(List<EnrollmentSourceRow> enrollmentsBySource) { this.enrollmentsBySource = enrollmentsBySource; }
    public List<EnrollmentRuleRow> getTopEnrollmentRules() { return topEnrollmentRules; }
    public void setTopEnrollmentRules(List<EnrollmentRuleRow> topEnrollmentRules) { this.topEnrollmentRules = topEnrollmentRules; }
}
