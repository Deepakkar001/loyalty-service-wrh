package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;

public class MerchantOpsSummaryResponse {

    private int activeCampaigns;
    private int pendingApprovalCampaigns;
    private int totalCampaigns;
    private BigDecimal totalBudgetAllocated;
    private BigDecimal totalBudgetConsumed;
    private long totalParticipations;
    private boolean integrationTestPassed;

    public int getActiveCampaigns() { return activeCampaigns; }
    public void setActiveCampaigns(int activeCampaigns) { this.activeCampaigns = activeCampaigns; }

    public int getPendingApprovalCampaigns() { return pendingApprovalCampaigns; }
    public void setPendingApprovalCampaigns(int pendingApprovalCampaigns) {
        this.pendingApprovalCampaigns = pendingApprovalCampaigns;
    }

    public int getTotalCampaigns() { return totalCampaigns; }
    public void setTotalCampaigns(int totalCampaigns) { this.totalCampaigns = totalCampaigns; }

    public BigDecimal getTotalBudgetAllocated() { return totalBudgetAllocated; }
    public void setTotalBudgetAllocated(BigDecimal totalBudgetAllocated) {
        this.totalBudgetAllocated = totalBudgetAllocated;
    }

    public BigDecimal getTotalBudgetConsumed() { return totalBudgetConsumed; }
    public void setTotalBudgetConsumed(BigDecimal totalBudgetConsumed) {
        this.totalBudgetConsumed = totalBudgetConsumed;
    }

    public long getTotalParticipations() { return totalParticipations; }
    public void setTotalParticipations(long totalParticipations) {
        this.totalParticipations = totalParticipations;
    }

    public boolean isIntegrationTestPassed() { return integrationTestPassed; }
    public void setIntegrationTestPassed(boolean integrationTestPassed) {
        this.integrationTestPassed = integrationTestPassed;
    }
}
