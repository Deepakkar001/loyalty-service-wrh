package com.loyaltyos.campaigns.dto;

import java.math.BigDecimal;

public class CampaignPerformanceSummary {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private int totalCampaigns;
    private int activeCampaigns;
    private long participationsInPeriod;
    private long participationsPriorPeriod;
    private long uniqueCustomersInPeriod;
    private BigDecimal pointsInPeriod;
    private BigDecimal cashbackInPeriod;
    private BigDecimal totalBudgetAllocated;
    private BigDecimal totalBudgetConsumed;
    private BigDecimal periodOverPeriodChangePct;

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public int getTotalCampaigns() { return totalCampaigns; }
    public void setTotalCampaigns(int totalCampaigns) { this.totalCampaigns = totalCampaigns; }
    public int getActiveCampaigns() { return activeCampaigns; }
    public void setActiveCampaigns(int activeCampaigns) { this.activeCampaigns = activeCampaigns; }
    public long getParticipationsInPeriod() { return participationsInPeriod; }
    public void setParticipationsInPeriod(long participationsInPeriod) { this.participationsInPeriod = participationsInPeriod; }
    public long getParticipationsPriorPeriod() { return participationsPriorPeriod; }
    public void setParticipationsPriorPeriod(long participationsPriorPeriod) { this.participationsPriorPeriod = participationsPriorPeriod; }
    public long getUniqueCustomersInPeriod() { return uniqueCustomersInPeriod; }
    public void setUniqueCustomersInPeriod(long uniqueCustomersInPeriod) { this.uniqueCustomersInPeriod = uniqueCustomersInPeriod; }
    public BigDecimal getPointsInPeriod() { return pointsInPeriod; }
    public void setPointsInPeriod(BigDecimal pointsInPeriod) { this.pointsInPeriod = pointsInPeriod; }
    public BigDecimal getCashbackInPeriod() { return cashbackInPeriod; }
    public void setCashbackInPeriod(BigDecimal cashbackInPeriod) { this.cashbackInPeriod = cashbackInPeriod; }
    public BigDecimal getTotalBudgetAllocated() { return totalBudgetAllocated; }
    public void setTotalBudgetAllocated(BigDecimal totalBudgetAllocated) { this.totalBudgetAllocated = totalBudgetAllocated; }
    public BigDecimal getTotalBudgetConsumed() { return totalBudgetConsumed; }
    public void setTotalBudgetConsumed(BigDecimal totalBudgetConsumed) { this.totalBudgetConsumed = totalBudgetConsumed; }
    public BigDecimal getPeriodOverPeriodChangePct() { return periodOverPeriodChangePct; }
    public void setPeriodOverPeriodChangePct(BigDecimal periodOverPeriodChangePct) { this.periodOverPeriodChangePct = periodOverPeriodChangePct; }
}
