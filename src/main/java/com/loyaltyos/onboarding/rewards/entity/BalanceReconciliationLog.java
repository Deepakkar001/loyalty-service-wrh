package com.loyaltyos.onboarding.rewards.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "balance_reconciliation_logs",
    indexes = {
        @Index(name = "idx_brl_tenant_programme", columnList = "tenant_id,programme_uid"),
        @Index(name = "idx_brl_tenant_customer", columnList = "tenant_id,customer_id")
    }
)
public class BalanceReconciliationLog {

    public static final String ACTION_NONE = "NONE";
    public static final String ACTION_UPDATED_CACHE = "UPDATED_CACHE";
    public static final String ACTION_MANUAL_REVIEW = "MANUAL_REVIEW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "customer_id", length = 128)
    private String customerId;

    @Column(name = "expected_balance", precision = 18, scale = 4)
    private BigDecimal expectedBalance;

    @Column(name = "cached_balance", precision = 18, scale = 4)
    private BigDecimal cachedBalance;

    @Column(name = "variance", precision = 18, scale = 4)
    private BigDecimal variance;

    @Column(name = "reconciliation_action", nullable = false, length = 32)
    private String reconciliationAction;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    public BalanceReconciliationLog() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getExpectedBalance() {
        return expectedBalance;
    }

    public void setExpectedBalance(BigDecimal expectedBalance) {
        this.expectedBalance = expectedBalance;
    }

    public BigDecimal getCachedBalance() {
        return cachedBalance;
    }

    public void setCachedBalance(BigDecimal cachedBalance) {
        this.cachedBalance = cachedBalance;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public String getReconciliationAction() {
        return reconciliationAction;
    }

    public void setReconciliationAction(String reconciliationAction) {
        this.reconciliationAction = reconciliationAction;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
