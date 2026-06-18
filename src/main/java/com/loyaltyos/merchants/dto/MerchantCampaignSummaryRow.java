package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;

public class MerchantCampaignSummaryRow {

    private String campaignUid;
    private String name;
    private String status;
    private BigDecimal budgetTotal;
    private BigDecimal budgetConsumed;
    private BigDecimal budgetConsumedPct;
    private long participations;
    private boolean pendingMerchantApproval;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }

    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }

    public BigDecimal getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(BigDecimal budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }

    public long getParticipations() { return participations; }
    public void setParticipations(long participations) { this.participations = participations; }

    public boolean isPendingMerchantApproval() { return pendingMerchantApproval; }
    public void setPendingMerchantApproval(boolean pendingMerchantApproval) {
        this.pendingMerchantApproval = pendingMerchantApproval;
    }
}
