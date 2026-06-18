package com.loyaltyos.merchants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "merchant_budget_alerts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_merchant_campaign_alert",
        columnNames = {"tenant_id", "campaign_uid", "alert_threshold_pct"}
    )
)
public class MerchantBudgetAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "merchant_uid", nullable = false, length = 128)
    private String merchantUid;

    @Column(name = "campaign_uid", nullable = false, length = 128)
    private String campaignUid;

    @Column(name = "alert_threshold_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal alertThresholdPct;

    @Column(name = "budget_consumed", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetConsumed;

    @Column(name = "budget_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetTotal;

    @CreationTimestamp
    @Column(name = "notified_at", nullable = false, updatable = false)
    private Instant notifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }

    public BigDecimal getAlertThresholdPct() { return alertThresholdPct; }
    public void setAlertThresholdPct(BigDecimal alertThresholdPct) { this.alertThresholdPct = alertThresholdPct; }

    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }

    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }

    public Instant getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; }
}
