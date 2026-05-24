package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class IntegrationBalanceResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    private BigDecimal balance;
    private BigDecimal ledgerDerivedBalance;
    private Instant updatedAt;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getLedgerDerivedBalance() { return ledgerDerivedBalance; }
    public void setLedgerDerivedBalance(BigDecimal ledgerDerivedBalance) {
        this.ledgerDerivedBalance = ledgerDerivedBalance;
    }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
