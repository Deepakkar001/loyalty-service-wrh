package com.loyaltyos.onboarding.rewards.dto;

import java.math.BigDecimal;

public class RewardBalanceDetailResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    private BigDecimal cachedBalance;
    private BigDecimal ledgerDerivedBalance;
    private BigDecimal variance;
    private BigDecimal expiringWithin7Days;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getCachedBalance() {
        return cachedBalance;
    }

    public void setCachedBalance(BigDecimal cachedBalance) {
        this.cachedBalance = cachedBalance;
    }

    public BigDecimal getLedgerDerivedBalance() {
        return ledgerDerivedBalance;
    }

    public void setLedgerDerivedBalance(BigDecimal ledgerDerivedBalance) {
        this.ledgerDerivedBalance = ledgerDerivedBalance;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public BigDecimal getExpiringWithin7Days() {
        return expiringWithin7Days;
    }

    public void setExpiringWithin7Days(BigDecimal expiringWithin7Days) {
        this.expiringWithin7Days = expiringWithin7Days;
    }
}
