package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class BalanceReconciliationVarianceRow {

    private String customerId;
    private BigDecimal expectedBalance;
    private BigDecimal cachedBalance;
    private BigDecimal variance;
    private String reconciliationAction;
    private Instant executedAt;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getExpectedBalance() { return expectedBalance; }
    public void setExpectedBalance(BigDecimal expectedBalance) { this.expectedBalance = expectedBalance; }
    public BigDecimal getCachedBalance() { return cachedBalance; }
    public void setCachedBalance(BigDecimal cachedBalance) { this.cachedBalance = cachedBalance; }
    public BigDecimal getVariance() { return variance; }
    public void setVariance(BigDecimal variance) { this.variance = variance; }
    public String getReconciliationAction() { return reconciliationAction; }
    public void setReconciliationAction(String reconciliationAction) { this.reconciliationAction = reconciliationAction; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
