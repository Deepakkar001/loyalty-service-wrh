package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public class BreakageMonthlyRow {

    private String month;
    private BigDecimal expiredPoints;
    private long customersAffected;
    private long transactionCount;

    public BreakageMonthlyRow() {}

    public BreakageMonthlyRow(String month, BigDecimal expiredPoints, long customersAffected, long transactionCount) {
        this.month = month;
        this.expiredPoints = expiredPoints;
        this.customersAffected = customersAffected;
        this.transactionCount = transactionCount;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public BigDecimal getExpiredPoints() { return expiredPoints; }
    public void setExpiredPoints(BigDecimal expiredPoints) { this.expiredPoints = expiredPoints; }
    public long getCustomersAffected() { return customersAffected; }
    public void setCustomersAffected(long customersAffected) { this.customersAffected = customersAffected; }
    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }
}
