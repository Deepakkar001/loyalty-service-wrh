package com.loyaltyos.merchants.entity;

import com.loyaltyos.merchants.enums.MerchantSettlementCycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "merchant_settlement_cycles",
    uniqueConstraints = @UniqueConstraint(name = "uk_settlement_cycle_uid", columnNames = "cycle_uid"),
    indexes = {
        @Index(name = "idx_settlement_merchant", columnList = "tenant_id, merchant_uid"),
        @Index(name = "idx_settlement_status", columnList = "tenant_id, status")
    }
)
public class MerchantSettlementCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_uid", nullable = false, length = 128)
    private String cycleUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "merchant_uid", nullable = false, length = 128)
    private String merchantUid;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MerchantSettlementCycleStatus status = MerchantSettlementCycleStatus.DRAFT;

    @Column(name = "total_points", nullable = false)
    private long totalPoints;

    @Column(name = "total_monetary_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalMonetaryValue = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCycleUid() { return cycleUid; }
    public void setCycleUid(String cycleUid) { this.cycleUid = cycleUid; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public MerchantSettlementCycleStatus getStatus() { return status; }
    public void setStatus(MerchantSettlementCycleStatus status) { this.status = status; }

    public long getTotalPoints() { return totalPoints; }
    public void setTotalPoints(long totalPoints) { this.totalPoints = totalPoints; }

    public BigDecimal getTotalMonetaryValue() { return totalMonetaryValue; }
    public void setTotalMonetaryValue(BigDecimal totalMonetaryValue) {
        this.totalMonetaryValue = totalMonetaryValue;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }
}
