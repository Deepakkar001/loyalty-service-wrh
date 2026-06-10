package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BreakageExpiryReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String currency;
    private BigDecimal pointsCurrencyRate;

    private BigDecimal pointsExpiredInPeriod;
    private long customersAffectedInPeriod;
    private long expireTransactionCount;
    private BigDecimal monetaryBreakageInPeriod;

    private BigDecimal pointsExpiredYtd;
    private BigDecimal monetaryBreakageYtd;

    private BigDecimal outstandingPointsLiability;
    private BigDecimal outstandingMonetaryLiability;

    private BigDecimal pointsExpiringNext30Days;
    private BigDecimal pointsExpiringNext60Days;
    private BigDecimal pointsExpiringNext90Days;

    private List<BreakageMonthlyRow> monthlyBreakage = new ArrayList<>();
    private List<BreakageTierRow> breakageByTier = new ArrayList<>();
    private List<UpcomingExpiryMonthRow> upcomingExpiryByMonth = new ArrayList<>();
    private List<ExpiryJobRunRow> recentExpiryJobRuns = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getPointsCurrencyRate() { return pointsCurrencyRate; }
    public void setPointsCurrencyRate(BigDecimal pointsCurrencyRate) { this.pointsCurrencyRate = pointsCurrencyRate; }
    public BigDecimal getPointsExpiredInPeriod() { return pointsExpiredInPeriod; }
    public void setPointsExpiredInPeriod(BigDecimal pointsExpiredInPeriod) { this.pointsExpiredInPeriod = pointsExpiredInPeriod; }
    public long getCustomersAffectedInPeriod() { return customersAffectedInPeriod; }
    public void setCustomersAffectedInPeriod(long customersAffectedInPeriod) { this.customersAffectedInPeriod = customersAffectedInPeriod; }
    public long getExpireTransactionCount() { return expireTransactionCount; }
    public void setExpireTransactionCount(long expireTransactionCount) { this.expireTransactionCount = expireTransactionCount; }
    public BigDecimal getMonetaryBreakageInPeriod() { return monetaryBreakageInPeriod; }
    public void setMonetaryBreakageInPeriod(BigDecimal monetaryBreakageInPeriod) { this.monetaryBreakageInPeriod = monetaryBreakageInPeriod; }
    public BigDecimal getPointsExpiredYtd() { return pointsExpiredYtd; }
    public void setPointsExpiredYtd(BigDecimal pointsExpiredYtd) { this.pointsExpiredYtd = pointsExpiredYtd; }
    public BigDecimal getMonetaryBreakageYtd() { return monetaryBreakageYtd; }
    public void setMonetaryBreakageYtd(BigDecimal monetaryBreakageYtd) { this.monetaryBreakageYtd = monetaryBreakageYtd; }
    public BigDecimal getOutstandingPointsLiability() { return outstandingPointsLiability; }
    public void setOutstandingPointsLiability(BigDecimal outstandingPointsLiability) { this.outstandingPointsLiability = outstandingPointsLiability; }
    public BigDecimal getOutstandingMonetaryLiability() { return outstandingMonetaryLiability; }
    public void setOutstandingMonetaryLiability(BigDecimal outstandingMonetaryLiability) { this.outstandingMonetaryLiability = outstandingMonetaryLiability; }
    public BigDecimal getPointsExpiringNext30Days() { return pointsExpiringNext30Days; }
    public void setPointsExpiringNext30Days(BigDecimal pointsExpiringNext30Days) { this.pointsExpiringNext30Days = pointsExpiringNext30Days; }
    public BigDecimal getPointsExpiringNext60Days() { return pointsExpiringNext60Days; }
    public void setPointsExpiringNext60Days(BigDecimal pointsExpiringNext60Days) { this.pointsExpiringNext60Days = pointsExpiringNext60Days; }
    public BigDecimal getPointsExpiringNext90Days() { return pointsExpiringNext90Days; }
    public void setPointsExpiringNext90Days(BigDecimal pointsExpiringNext90Days) { this.pointsExpiringNext90Days = pointsExpiringNext90Days; }
    public List<BreakageMonthlyRow> getMonthlyBreakage() { return monthlyBreakage; }
    public void setMonthlyBreakage(List<BreakageMonthlyRow> monthlyBreakage) { this.monthlyBreakage = monthlyBreakage; }
    public List<BreakageTierRow> getBreakageByTier() { return breakageByTier; }
    public void setBreakageByTier(List<BreakageTierRow> breakageByTier) { this.breakageByTier = breakageByTier; }
    public List<UpcomingExpiryMonthRow> getUpcomingExpiryByMonth() { return upcomingExpiryByMonth; }
    public void setUpcomingExpiryByMonth(List<UpcomingExpiryMonthRow> upcomingExpiryByMonth) { this.upcomingExpiryByMonth = upcomingExpiryByMonth; }
    public List<ExpiryJobRunRow> getRecentExpiryJobRuns() { return recentExpiryJobRuns; }
    public void setRecentExpiryJobRuns(List<ExpiryJobRunRow> recentExpiryJobRuns) { this.recentExpiryJobRuns = recentExpiryJobRuns; }
}
