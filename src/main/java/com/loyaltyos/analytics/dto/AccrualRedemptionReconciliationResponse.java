package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccrualRedemptionReconciliationResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String currency;
    private BigDecimal pointsCurrencyRate;
    private String reportDefinition;

    private BigDecimal openingPointsLiability;
    private BigDecimal openingMonetaryLiability;
    private BigDecimal closingPointsLiabilityLedger;
    private BigDecimal closingMonetaryLiabilityLedger;
    private BigDecimal closingPointsLiabilityCache;
    private BigDecimal closingMonetaryLiabilityCache;
    private BigDecimal calculatedClosingPoints;
    private BigDecimal waterfallVariancePoints;
    private BigDecimal cacheVsLedgerVariancePoints;
    private String reconciliationStatus;

    private BigDecimal accrualsPoints;
    private BigDecimal redemptionsPoints;
    private BigDecimal expirationsPoints;
    private BigDecimal reversalsPoints;
    private BigDecimal adjustmentsNetPoints;
    private BigDecimal netChangePoints;
    private BigDecimal netChangeMonetary;

    private BigDecimal priorPeriodNetChangePoints;
    private BigDecimal periodOverPeriodNetChangePct;

    private long totalTransactionsInPeriod;
    private long uniqueCustomersInPeriod;

    private List<ReconciliationMovementRow> movements = new ArrayList<>();
    private List<ReconciliationDailyRow> dailyTrend = new ArrayList<>();
    private List<BalanceReconciliationVarianceRow> recentBalanceVariances = new ArrayList<>();

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
    public String getReportDefinition() { return reportDefinition; }
    public void setReportDefinition(String reportDefinition) { this.reportDefinition = reportDefinition; }
    public BigDecimal getOpeningPointsLiability() { return openingPointsLiability; }
    public void setOpeningPointsLiability(BigDecimal openingPointsLiability) { this.openingPointsLiability = openingPointsLiability; }
    public BigDecimal getOpeningMonetaryLiability() { return openingMonetaryLiability; }
    public void setOpeningMonetaryLiability(BigDecimal openingMonetaryLiability) { this.openingMonetaryLiability = openingMonetaryLiability; }
    public BigDecimal getClosingPointsLiabilityLedger() { return closingPointsLiabilityLedger; }
    public void setClosingPointsLiabilityLedger(BigDecimal closingPointsLiabilityLedger) { this.closingPointsLiabilityLedger = closingPointsLiabilityLedger; }
    public BigDecimal getClosingMonetaryLiabilityLedger() { return closingMonetaryLiabilityLedger; }
    public void setClosingMonetaryLiabilityLedger(BigDecimal closingMonetaryLiabilityLedger) { this.closingMonetaryLiabilityLedger = closingMonetaryLiabilityLedger; }
    public BigDecimal getClosingPointsLiabilityCache() { return closingPointsLiabilityCache; }
    public void setClosingPointsLiabilityCache(BigDecimal closingPointsLiabilityCache) { this.closingPointsLiabilityCache = closingPointsLiabilityCache; }
    public BigDecimal getClosingMonetaryLiabilityCache() { return closingMonetaryLiabilityCache; }
    public void setClosingMonetaryLiabilityCache(BigDecimal closingMonetaryLiabilityCache) { this.closingMonetaryLiabilityCache = closingMonetaryLiabilityCache; }
    public BigDecimal getCalculatedClosingPoints() { return calculatedClosingPoints; }
    public void setCalculatedClosingPoints(BigDecimal calculatedClosingPoints) { this.calculatedClosingPoints = calculatedClosingPoints; }
    public BigDecimal getWaterfallVariancePoints() { return waterfallVariancePoints; }
    public void setWaterfallVariancePoints(BigDecimal waterfallVariancePoints) { this.waterfallVariancePoints = waterfallVariancePoints; }
    public BigDecimal getCacheVsLedgerVariancePoints() { return cacheVsLedgerVariancePoints; }
    public void setCacheVsLedgerVariancePoints(BigDecimal cacheVsLedgerVariancePoints) { this.cacheVsLedgerVariancePoints = cacheVsLedgerVariancePoints; }
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public BigDecimal getAccrualsPoints() { return accrualsPoints; }
    public void setAccrualsPoints(BigDecimal accrualsPoints) { this.accrualsPoints = accrualsPoints; }
    public BigDecimal getRedemptionsPoints() { return redemptionsPoints; }
    public void setRedemptionsPoints(BigDecimal redemptionsPoints) { this.redemptionsPoints = redemptionsPoints; }
    public BigDecimal getExpirationsPoints() { return expirationsPoints; }
    public void setExpirationsPoints(BigDecimal expirationsPoints) { this.expirationsPoints = expirationsPoints; }
    public BigDecimal getReversalsPoints() { return reversalsPoints; }
    public void setReversalsPoints(BigDecimal reversalsPoints) { this.reversalsPoints = reversalsPoints; }
    public BigDecimal getAdjustmentsNetPoints() { return adjustmentsNetPoints; }
    public void setAdjustmentsNetPoints(BigDecimal adjustmentsNetPoints) { this.adjustmentsNetPoints = adjustmentsNetPoints; }
    public BigDecimal getNetChangePoints() { return netChangePoints; }
    public void setNetChangePoints(BigDecimal netChangePoints) { this.netChangePoints = netChangePoints; }
    public BigDecimal getNetChangeMonetary() { return netChangeMonetary; }
    public void setNetChangeMonetary(BigDecimal netChangeMonetary) { this.netChangeMonetary = netChangeMonetary; }
    public BigDecimal getPriorPeriodNetChangePoints() { return priorPeriodNetChangePoints; }
    public void setPriorPeriodNetChangePoints(BigDecimal priorPeriodNetChangePoints) { this.priorPeriodNetChangePoints = priorPeriodNetChangePoints; }
    public BigDecimal getPeriodOverPeriodNetChangePct() { return periodOverPeriodNetChangePct; }
    public void setPeriodOverPeriodNetChangePct(BigDecimal periodOverPeriodNetChangePct) { this.periodOverPeriodNetChangePct = periodOverPeriodNetChangePct; }
    public long getTotalTransactionsInPeriod() { return totalTransactionsInPeriod; }
    public void setTotalTransactionsInPeriod(long totalTransactionsInPeriod) { this.totalTransactionsInPeriod = totalTransactionsInPeriod; }
    public long getUniqueCustomersInPeriod() { return uniqueCustomersInPeriod; }
    public void setUniqueCustomersInPeriod(long uniqueCustomersInPeriod) { this.uniqueCustomersInPeriod = uniqueCustomersInPeriod; }
    public List<ReconciliationMovementRow> getMovements() { return movements; }
    public void setMovements(List<ReconciliationMovementRow> movements) { this.movements = movements; }
    public List<ReconciliationDailyRow> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<ReconciliationDailyRow> dailyTrend) { this.dailyTrend = dailyTrend; }
    public List<BalanceReconciliationVarianceRow> getRecentBalanceVariances() { return recentBalanceVariances; }
    public void setRecentBalanceVariances(List<BalanceReconciliationVarianceRow> recentBalanceVariances) { this.recentBalanceVariances = recentBalanceVariances; }
}
