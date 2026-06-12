package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FailedAccrualRedemptionReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String reportDefinition;

    private long failedAccrualsInPeriod;
    private long failedRedemptionsInPeriod;
    private long totalFailuresInPeriod;
    private long failedAccrualsPriorPeriod;
    private long failedRedemptionsPriorPeriod;
    private BigDecimal periodOverPeriodChangePct;

    private long accrualAttemptsInPeriod;
    private long redemptionApiAttemptsInPeriod;
    private BigDecimal accrualFailureRatePct;
    private BigDecimal redemptionFailureRatePct;

    private Integer avgFailedAccrualDurationMs;
    private Integer avgFailedRedemptionDurationMs;

    private List<FailureCategoryRow> failuresByCategory = new ArrayList<>();
    private List<FailureDailyTrendRow> dailyTrend = new ArrayList<>();
    private List<FailedTransactionRow> recentFailures = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getReportDefinition() { return reportDefinition; }
    public void setReportDefinition(String reportDefinition) { this.reportDefinition = reportDefinition; }
    public long getFailedAccrualsInPeriod() { return failedAccrualsInPeriod; }
    public void setFailedAccrualsInPeriod(long failedAccrualsInPeriod) { this.failedAccrualsInPeriod = failedAccrualsInPeriod; }
    public long getFailedRedemptionsInPeriod() { return failedRedemptionsInPeriod; }
    public void setFailedRedemptionsInPeriod(long failedRedemptionsInPeriod) { this.failedRedemptionsInPeriod = failedRedemptionsInPeriod; }
    public long getTotalFailuresInPeriod() { return totalFailuresInPeriod; }
    public void setTotalFailuresInPeriod(long totalFailuresInPeriod) { this.totalFailuresInPeriod = totalFailuresInPeriod; }
    public long getFailedAccrualsPriorPeriod() { return failedAccrualsPriorPeriod; }
    public void setFailedAccrualsPriorPeriod(long failedAccrualsPriorPeriod) { this.failedAccrualsPriorPeriod = failedAccrualsPriorPeriod; }
    public long getFailedRedemptionsPriorPeriod() { return failedRedemptionsPriorPeriod; }
    public void setFailedRedemptionsPriorPeriod(long failedRedemptionsPriorPeriod) { this.failedRedemptionsPriorPeriod = failedRedemptionsPriorPeriod; }
    public BigDecimal getPeriodOverPeriodChangePct() { return periodOverPeriodChangePct; }
    public void setPeriodOverPeriodChangePct(BigDecimal periodOverPeriodChangePct) { this.periodOverPeriodChangePct = periodOverPeriodChangePct; }
    public long getAccrualAttemptsInPeriod() { return accrualAttemptsInPeriod; }
    public void setAccrualAttemptsInPeriod(long accrualAttemptsInPeriod) { this.accrualAttemptsInPeriod = accrualAttemptsInPeriod; }
    public long getRedemptionApiAttemptsInPeriod() { return redemptionApiAttemptsInPeriod; }
    public void setRedemptionApiAttemptsInPeriod(long redemptionApiAttemptsInPeriod) { this.redemptionApiAttemptsInPeriod = redemptionApiAttemptsInPeriod; }
    public BigDecimal getAccrualFailureRatePct() { return accrualFailureRatePct; }
    public void setAccrualFailureRatePct(BigDecimal accrualFailureRatePct) { this.accrualFailureRatePct = accrualFailureRatePct; }
    public BigDecimal getRedemptionFailureRatePct() { return redemptionFailureRatePct; }
    public void setRedemptionFailureRatePct(BigDecimal redemptionFailureRatePct) { this.redemptionFailureRatePct = redemptionFailureRatePct; }
    public Integer getAvgFailedAccrualDurationMs() { return avgFailedAccrualDurationMs; }
    public void setAvgFailedAccrualDurationMs(Integer avgFailedAccrualDurationMs) { this.avgFailedAccrualDurationMs = avgFailedAccrualDurationMs; }
    public Integer getAvgFailedRedemptionDurationMs() { return avgFailedRedemptionDurationMs; }
    public void setAvgFailedRedemptionDurationMs(Integer avgFailedRedemptionDurationMs) { this.avgFailedRedemptionDurationMs = avgFailedRedemptionDurationMs; }
    public List<FailureCategoryRow> getFailuresByCategory() { return failuresByCategory; }
    public void setFailuresByCategory(List<FailureCategoryRow> failuresByCategory) { this.failuresByCategory = failuresByCategory; }
    public List<FailureDailyTrendRow> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<FailureDailyTrendRow> dailyTrend) { this.dailyTrend = dailyTrend; }
    public List<FailedTransactionRow> getRecentFailures() { return recentFailures; }
    public void setRecentFailures(List<FailedTransactionRow> recentFailures) { this.recentFailures = recentFailures; }
}
