package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public class UpcomingExpiryMonthRow {

    private String expiryMonth;
    private BigDecimal pointsExpiring;
    private long customersAffected;

    public UpcomingExpiryMonthRow() {}

    public UpcomingExpiryMonthRow(String expiryMonth, BigDecimal pointsExpiring, long customersAffected) {
        this.expiryMonth = expiryMonth;
        this.pointsExpiring = pointsExpiring;
        this.customersAffected = customersAffected;
    }

    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }
    public BigDecimal getPointsExpiring() { return pointsExpiring; }
    public void setPointsExpiring(BigDecimal pointsExpiring) { this.pointsExpiring = pointsExpiring; }
    public long getCustomersAffected() { return customersAffected; }
    public void setCustomersAffected(long customersAffected) { this.customersAffected = customersAffected; }
}
