package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MerchantDashboardStatsResponse {

    private int activeCampaigns;
    private int pendingApprovalCampaigns;
    private BigDecimal totalBudgetAllocated = BigDecimal.ZERO;
    private BigDecimal totalBudgetConsumed = BigDecimal.ZERO;
    private BigDecimal budgetConsumedPct = BigDecimal.ZERO;
    private long totalParticipations;
    private BigDecimal totalPointsIssued = BigDecimal.ZERO;
    private List<MerchantCampaignSummaryRow> recentCampaigns = new ArrayList<>();

    public int getActiveCampaigns() { return activeCampaigns; }
    public void setActiveCampaigns(int activeCampaigns) { this.activeCampaigns = activeCampaigns; }

    public int getPendingApprovalCampaigns() { return pendingApprovalCampaigns; }
    public void setPendingApprovalCampaigns(int pendingApprovalCampaigns) {
        this.pendingApprovalCampaigns = pendingApprovalCampaigns;
    }

    public BigDecimal getTotalBudgetAllocated() { return totalBudgetAllocated; }
    public void setTotalBudgetAllocated(BigDecimal totalBudgetAllocated) {
        this.totalBudgetAllocated = totalBudgetAllocated;
    }

    public BigDecimal getTotalBudgetConsumed() { return totalBudgetConsumed; }
    public void setTotalBudgetConsumed(BigDecimal totalBudgetConsumed) {
        this.totalBudgetConsumed = totalBudgetConsumed;
    }

    public BigDecimal getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(BigDecimal budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }

    public long getTotalParticipations() { return totalParticipations; }
    public void setTotalParticipations(long totalParticipations) { this.totalParticipations = totalParticipations; }

    public BigDecimal getTotalPointsIssued() { return totalPointsIssued; }
    public void setTotalPointsIssued(BigDecimal totalPointsIssued) { this.totalPointsIssued = totalPointsIssued; }

    public List<MerchantCampaignSummaryRow> getRecentCampaigns() { return recentCampaigns; }
    public void setRecentCampaigns(List<MerchantCampaignSummaryRow> recentCampaigns) {
        this.recentCampaigns = recentCampaigns;
    }
}
