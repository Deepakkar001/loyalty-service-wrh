package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public class BreakageTierRow {

    private String tierName;
    private Integer rankOrder;
    private BigDecimal expiredPoints;
    private long customersAffected;

    public BreakageTierRow() {}

    public BreakageTierRow(String tierName, Integer rankOrder, BigDecimal expiredPoints, long customersAffected) {
        this.tierName = tierName;
        this.rankOrder = rankOrder;
        this.expiredPoints = expiredPoints;
        this.customersAffected = customersAffected;
    }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }
    public BigDecimal getExpiredPoints() { return expiredPoints; }
    public void setExpiredPoints(BigDecimal expiredPoints) { this.expiredPoints = expiredPoints; }
    public long getCustomersAffected() { return customersAffected; }
    public void setCustomersAffected(long customersAffected) { this.customersAffected = customersAffected; }
}
