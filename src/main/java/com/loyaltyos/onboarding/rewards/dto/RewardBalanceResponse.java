package com.loyaltyos.onboarding.rewards.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Balance read model: {@code balance} is the materialized cache (fast path for UI).
 * {@code ledgerDerivedBalance} is the signed net from {@code points_ledger}; {@code balanceVariance} is ledger minus cache.
 */
public class RewardBalanceResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    /** Materialized cache balance (same meaning as before this field split). */
    private BigDecimal balance;
    private Instant updatedAt;
    /** Authoritative signed sum from ledger for this tenant, programme, and customer. */
    private BigDecimal ledgerDerivedBalance;
    /** {@code ledgerDerivedBalance - balance} (positive means cache is low). */
    private BigDecimal balanceVariance;

    public RewardBalanceResponse() {}

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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getLedgerDerivedBalance() {
        return ledgerDerivedBalance;
    }

    public void setLedgerDerivedBalance(BigDecimal ledgerDerivedBalance) {
        this.ledgerDerivedBalance = ledgerDerivedBalance;
    }

    public BigDecimal getBalanceVariance() {
        return balanceVariance;
    }

    public void setBalanceVariance(BigDecimal balanceVariance) {
        this.balanceVariance = balanceVariance;
    }
}
