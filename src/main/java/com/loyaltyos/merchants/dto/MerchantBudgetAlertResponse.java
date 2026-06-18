package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class MerchantBudgetAlertResponse {

    private String campaignUid;
    private BigDecimal alertThresholdPct;
    private BigDecimal budgetConsumed;
    private BigDecimal budgetTotal;
    private Instant notifiedAt;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }

    public BigDecimal getAlertThresholdPct() { return alertThresholdPct; }
    public void setAlertThresholdPct(BigDecimal alertThresholdPct) {
        this.alertThresholdPct = alertThresholdPct;
    }

    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }

    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }

    public Instant getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; }
}
