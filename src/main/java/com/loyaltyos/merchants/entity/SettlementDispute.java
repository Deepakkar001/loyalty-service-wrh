package com.loyaltyos.merchants.entity;

import com.loyaltyos.merchants.enums.SettlementDisputeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "settlement_disputes",
    uniqueConstraints = @UniqueConstraint(name = "uk_settlement_dispute_uid", columnNames = "dispute_uid")
)
public class SettlementDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_uid", nullable = false, length = 128)
    private String disputeUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_item_id", nullable = false)
    private SettlementLineItem lineItem;

    @Column(name = "reason", nullable = false, length = 1024)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SettlementDisputeStatus status = SettlementDisputeStatus.OPEN;

    @Column(name = "hold_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal holdAmount = BigDecimal.ZERO;

    @Column(name = "resolution", length = 1024)
    private String resolution;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDisputeUid() { return disputeUid; }
    public void setDisputeUid(String disputeUid) { this.disputeUid = disputeUid; }

    public SettlementLineItem getLineItem() { return lineItem; }
    public void setLineItem(SettlementLineItem lineItem) { this.lineItem = lineItem; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public SettlementDisputeStatus getStatus() { return status; }
    public void setStatus(SettlementDisputeStatus status) { this.status = status; }

    public BigDecimal getHoldAmount() { return holdAmount; }
    public void setHoldAmount(BigDecimal holdAmount) { this.holdAmount = holdAmount; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
