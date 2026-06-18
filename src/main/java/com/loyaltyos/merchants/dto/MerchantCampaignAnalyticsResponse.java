package com.loyaltyos.merchants.dto;

import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import java.math.BigDecimal;
import java.util.List;

public class MerchantCampaignAnalyticsResponse {

    private long totalCampaigns;
    private long activeCampaigns;
    private long totalParticipations;
    private long totalUniqueCustomers;
    private BigDecimal totalPointsIssued;
    private BigDecimal totalCashbackRecorded;
    private BigDecimal totalBudgetAllocated;
    private BigDecimal totalBudgetConsumed;
    private double budgetConsumedPct;
    private List<CampaignStatsResponse> campaignStats;

    public long getTotalCampaigns() { return totalCampaigns; }
    public void setTotalCampaigns(long totalCampaigns) { this.totalCampaigns = totalCampaigns; }

    public long getActiveCampaigns() { return activeCampaigns; }
    public void setActiveCampaigns(long activeCampaigns) { this.activeCampaigns = activeCampaigns; }

    public long getTotalParticipations() { return totalParticipations; }
    public void setTotalParticipations(long totalParticipations) { this.totalParticipations = totalParticipations; }

    public long getTotalUniqueCustomers() { return totalUniqueCustomers; }
    public void setTotalUniqueCustomers(long totalUniqueCustomers) { this.totalUniqueCustomers = totalUniqueCustomers; }

    public BigDecimal getTotalPointsIssued() { return totalPointsIssued; }
    public void setTotalPointsIssued(BigDecimal totalPointsIssued) { this.totalPointsIssued = totalPointsIssued; }

    public BigDecimal getTotalCashbackRecorded() { return totalCashbackRecorded; }
    public void setTotalCashbackRecorded(BigDecimal totalCashbackRecorded) { this.totalCashbackRecorded = totalCashbackRecorded; }

    public BigDecimal getTotalBudgetAllocated() { return totalBudgetAllocated; }
    public void setTotalBudgetAllocated(BigDecimal totalBudgetAllocated) { this.totalBudgetAllocated = totalBudgetAllocated; }

    public BigDecimal getTotalBudgetConsumed() { return totalBudgetConsumed; }
    public void setTotalBudgetConsumed(BigDecimal totalBudgetConsumed) { this.totalBudgetConsumed = totalBudgetConsumed; }

    public double getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(double budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }

    public List<CampaignStatsResponse> getCampaignStats() { return campaignStats; }
    public void setCampaignStats(List<CampaignStatsResponse> campaignStats) { this.campaignStats = campaignStats; }
}
