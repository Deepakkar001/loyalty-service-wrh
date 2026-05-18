package com.loyaltyos.campaigns.dto;

import com.loyaltyos.campaigns.enums.CampaignStatus;
import java.math.BigDecimal;

public class CampaignStatsResponse {

    private String campaignUid;
    private String campaignName;
    private CampaignStatus status;
    private BigDecimal budgetTotal;
    private BigDecimal budgetConsumed;
    private BigDecimal budgetConsumedPct;
    private BigDecimal budgetRemaining;
    private long totalParticipations;
    private long uniqueCustomersReached;
    private BigDecimal totalPointsIssued;
    private BigDecimal totalCashbackRecorded;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }
    public BigDecimal getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(BigDecimal budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }
    public BigDecimal getBudgetRemaining() { return budgetRemaining; }
    public void setBudgetRemaining(BigDecimal budgetRemaining) { this.budgetRemaining = budgetRemaining; }
    public long getTotalParticipations() { return totalParticipations; }
    public void setTotalParticipations(long totalParticipations) { this.totalParticipations = totalParticipations; }
    public long getUniqueCustomersReached() { return uniqueCustomersReached; }
    public void setUniqueCustomersReached(long uniqueCustomersReached) { this.uniqueCustomersReached = uniqueCustomersReached; }
    public BigDecimal getTotalPointsIssued() { return totalPointsIssued; }
    public void setTotalPointsIssued(BigDecimal totalPointsIssued) { this.totalPointsIssued = totalPointsIssued; }
    public BigDecimal getTotalCashbackRecorded() { return totalCashbackRecorded; }
    public void setTotalCashbackRecorded(BigDecimal totalCashbackRecorded) { this.totalCashbackRecorded = totalCashbackRecorded; }
}
