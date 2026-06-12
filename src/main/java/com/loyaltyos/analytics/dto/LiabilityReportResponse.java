package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiabilityReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String currency;
    private BigDecimal pointsCurrencyRate;
    private String reportDefinition;

    private BigDecimal outstandingPointsLiability;
    private BigDecimal outstandingMonetaryLiability;
    private BigDecimal ledgerClosingPoints;
    private BigDecimal ledgerClosingMonetary;
    private BigDecimal ledgerVsCacheVariancePoints;

    private BigDecimal periodPointsIssued;
    private BigDecimal periodPointsRedeemed;
    private BigDecimal periodPointsExpired;
    private BigDecimal periodPointsReversed;
    private BigDecimal periodAdjustmentsNet;
    private BigDecimal periodNetChangePoints;
    private BigDecimal periodNetChangeMonetary;

    private BigDecimal openingPointsLiability;
    private BigDecimal openingMonetaryLiability;
    private BigDecimal closingPointsLiability;
    private BigDecimal closingMonetaryLiability;

    private long membersWithBalance;
    private long totalLedgerTransactionsInPeriod;

    private List<LiabilityMonthlyMovementRow> monthlyMovement = new ArrayList<>();
    private List<LiabilityProgrammeRollupRow> programmeRollups = new ArrayList<>();
    private LiabilityProgrammeRollupRow tenantRollup;
    private List<LiabilityTierBreakdownRow> liabilityByTier = new ArrayList<>();

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
    public BigDecimal getOutstandingPointsLiability() { return outstandingPointsLiability; }
    public void setOutstandingPointsLiability(BigDecimal outstandingPointsLiability) { this.outstandingPointsLiability = outstandingPointsLiability; }
    public BigDecimal getOutstandingMonetaryLiability() { return outstandingMonetaryLiability; }
    public void setOutstandingMonetaryLiability(BigDecimal outstandingMonetaryLiability) { this.outstandingMonetaryLiability = outstandingMonetaryLiability; }
    public BigDecimal getLedgerClosingPoints() { return ledgerClosingPoints; }
    public void setLedgerClosingPoints(BigDecimal ledgerClosingPoints) { this.ledgerClosingPoints = ledgerClosingPoints; }
    public BigDecimal getLedgerClosingMonetary() { return ledgerClosingMonetary; }
    public void setLedgerClosingMonetary(BigDecimal ledgerClosingMonetary) { this.ledgerClosingMonetary = ledgerClosingMonetary; }
    public BigDecimal getLedgerVsCacheVariancePoints() { return ledgerVsCacheVariancePoints; }
    public void setLedgerVsCacheVariancePoints(BigDecimal ledgerVsCacheVariancePoints) { this.ledgerVsCacheVariancePoints = ledgerVsCacheVariancePoints; }
    public BigDecimal getPeriodPointsIssued() { return periodPointsIssued; }
    public void setPeriodPointsIssued(BigDecimal periodPointsIssued) { this.periodPointsIssued = periodPointsIssued; }
    public BigDecimal getPeriodPointsRedeemed() { return periodPointsRedeemed; }
    public void setPeriodPointsRedeemed(BigDecimal periodPointsRedeemed) { this.periodPointsRedeemed = periodPointsRedeemed; }
    public BigDecimal getPeriodPointsExpired() { return periodPointsExpired; }
    public void setPeriodPointsExpired(BigDecimal periodPointsExpired) { this.periodPointsExpired = periodPointsExpired; }
    public BigDecimal getPeriodPointsReversed() { return periodPointsReversed; }
    public void setPeriodPointsReversed(BigDecimal periodPointsReversed) { this.periodPointsReversed = periodPointsReversed; }
    public BigDecimal getPeriodAdjustmentsNet() { return periodAdjustmentsNet; }
    public void setPeriodAdjustmentsNet(BigDecimal periodAdjustmentsNet) { this.periodAdjustmentsNet = periodAdjustmentsNet; }
    public BigDecimal getPeriodNetChangePoints() { return periodNetChangePoints; }
    public void setPeriodNetChangePoints(BigDecimal periodNetChangePoints) { this.periodNetChangePoints = periodNetChangePoints; }
    public BigDecimal getPeriodNetChangeMonetary() { return periodNetChangeMonetary; }
    public void setPeriodNetChangeMonetary(BigDecimal periodNetChangeMonetary) { this.periodNetChangeMonetary = periodNetChangeMonetary; }
    public BigDecimal getOpeningPointsLiability() { return openingPointsLiability; }
    public void setOpeningPointsLiability(BigDecimal openingPointsLiability) { this.openingPointsLiability = openingPointsLiability; }
    public BigDecimal getOpeningMonetaryLiability() { return openingMonetaryLiability; }
    public void setOpeningMonetaryLiability(BigDecimal openingMonetaryLiability) { this.openingMonetaryLiability = openingMonetaryLiability; }
    public BigDecimal getClosingPointsLiability() { return closingPointsLiability; }
    public void setClosingPointsLiability(BigDecimal closingPointsLiability) { this.closingPointsLiability = closingPointsLiability; }
    public BigDecimal getClosingMonetaryLiability() { return closingMonetaryLiability; }
    public void setClosingMonetaryLiability(BigDecimal closingMonetaryLiability) { this.closingMonetaryLiability = closingMonetaryLiability; }
    public long getMembersWithBalance() { return membersWithBalance; }
    public void setMembersWithBalance(long membersWithBalance) { this.membersWithBalance = membersWithBalance; }
    public long getTotalLedgerTransactionsInPeriod() { return totalLedgerTransactionsInPeriod; }
    public void setTotalLedgerTransactionsInPeriod(long totalLedgerTransactionsInPeriod) { this.totalLedgerTransactionsInPeriod = totalLedgerTransactionsInPeriod; }
    public List<LiabilityMonthlyMovementRow> getMonthlyMovement() { return monthlyMovement; }
    public void setMonthlyMovement(List<LiabilityMonthlyMovementRow> monthlyMovement) { this.monthlyMovement = monthlyMovement; }
    public List<LiabilityProgrammeRollupRow> getProgrammeRollups() { return programmeRollups; }
    public void setProgrammeRollups(List<LiabilityProgrammeRollupRow> programmeRollups) { this.programmeRollups = programmeRollups; }
    public LiabilityProgrammeRollupRow getTenantRollup() { return tenantRollup; }
    public void setTenantRollup(LiabilityProgrammeRollupRow tenantRollup) { this.tenantRollup = tenantRollup; }
    public List<LiabilityTierBreakdownRow> getLiabilityByTier() { return liabilityByTier; }
    public void setLiabilityByTier(List<LiabilityTierBreakdownRow> liabilityByTier) { this.liabilityByTier = liabilityByTier; }
}
