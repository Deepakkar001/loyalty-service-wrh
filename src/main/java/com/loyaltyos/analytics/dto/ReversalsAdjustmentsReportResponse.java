package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReversalsAdjustmentsReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String reportDefinition;

    private long reversalCountInPeriod;
    private BigDecimal reversalPointsInPeriod;
    private long adjustmentCountInPeriod;
    private BigDecimal adjustmentNetPointsInPeriod;
    private long uniqueCustomersAffected;
    private long reversalCountPriorPeriod;
    private long adjustmentCountPriorPeriod;
    private BigDecimal periodOverPeriodReversalChangePct;
    private BigDecimal periodOverPeriodAdjustmentChangePct;

    private List<ReversalAdjustmentDailyRow> dailyTrend = new ArrayList<>();
    private List<ReversalAdjustmentCustomerRow> topCustomers = new ArrayList<>();
    private List<ReversalAdjustmentLedgerRow> reversals = new ArrayList<>();
    private List<ReversalAdjustmentLedgerRow> adjustments = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getReportDefinition() { return reportDefinition; }
    public void setReportDefinition(String reportDefinition) { this.reportDefinition = reportDefinition; }
    public long getReversalCountInPeriod() { return reversalCountInPeriod; }
    public void setReversalCountInPeriod(long reversalCountInPeriod) { this.reversalCountInPeriod = reversalCountInPeriod; }
    public BigDecimal getReversalPointsInPeriod() { return reversalPointsInPeriod; }
    public void setReversalPointsInPeriod(BigDecimal reversalPointsInPeriod) { this.reversalPointsInPeriod = reversalPointsInPeriod; }
    public long getAdjustmentCountInPeriod() { return adjustmentCountInPeriod; }
    public void setAdjustmentCountInPeriod(long adjustmentCountInPeriod) { this.adjustmentCountInPeriod = adjustmentCountInPeriod; }
    public BigDecimal getAdjustmentNetPointsInPeriod() { return adjustmentNetPointsInPeriod; }
    public void setAdjustmentNetPointsInPeriod(BigDecimal adjustmentNetPointsInPeriod) { this.adjustmentNetPointsInPeriod = adjustmentNetPointsInPeriod; }
    public long getUniqueCustomersAffected() { return uniqueCustomersAffected; }
    public void setUniqueCustomersAffected(long uniqueCustomersAffected) { this.uniqueCustomersAffected = uniqueCustomersAffected; }
    public long getReversalCountPriorPeriod() { return reversalCountPriorPeriod; }
    public void setReversalCountPriorPeriod(long reversalCountPriorPeriod) { this.reversalCountPriorPeriod = reversalCountPriorPeriod; }
    public long getAdjustmentCountPriorPeriod() { return adjustmentCountPriorPeriod; }
    public void setAdjustmentCountPriorPeriod(long adjustmentCountPriorPeriod) { this.adjustmentCountPriorPeriod = adjustmentCountPriorPeriod; }
    public BigDecimal getPeriodOverPeriodReversalChangePct() { return periodOverPeriodReversalChangePct; }
    public void setPeriodOverPeriodReversalChangePct(BigDecimal periodOverPeriodReversalChangePct) { this.periodOverPeriodReversalChangePct = periodOverPeriodReversalChangePct; }
    public BigDecimal getPeriodOverPeriodAdjustmentChangePct() { return periodOverPeriodAdjustmentChangePct; }
    public void setPeriodOverPeriodAdjustmentChangePct(BigDecimal periodOverPeriodAdjustmentChangePct) { this.periodOverPeriodAdjustmentChangePct = periodOverPeriodAdjustmentChangePct; }
    public List<ReversalAdjustmentDailyRow> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<ReversalAdjustmentDailyRow> dailyTrend) { this.dailyTrend = dailyTrend; }
    public List<ReversalAdjustmentCustomerRow> getTopCustomers() { return topCustomers; }
    public void setTopCustomers(List<ReversalAdjustmentCustomerRow> topCustomers) { this.topCustomers = topCustomers; }
    public List<ReversalAdjustmentLedgerRow> getReversals() { return reversals; }
    public void setReversals(List<ReversalAdjustmentLedgerRow> reversals) { this.reversals = reversals; }
    public List<ReversalAdjustmentLedgerRow> getAdjustments() { return adjustments; }
    public void setAdjustments(List<ReversalAdjustmentLedgerRow> adjustments) { this.adjustments = adjustments; }
}
