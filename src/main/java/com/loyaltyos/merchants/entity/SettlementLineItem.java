package com.loyaltyos.merchants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "settlement_line_items",
    uniqueConstraints = @UniqueConstraint(name = "uk_settlement_line_item_uid", columnNames = "line_item_uid")
)
public class SettlementLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_item_uid", nullable = false, length = 128)
    private String lineItemUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private MerchantSettlementCycle cycle;

    @Column(name = "txn_reference", nullable = false, length = 256)
    private String txnReference;

    @Column(name = "points_amount", nullable = false)
    private long pointsAmount;

    @Column(name = "monetary_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal monetaryValue = BigDecimal.ZERO;

    @Column(name = "disputed", nullable = false)
    private boolean disputed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLineItemUid() { return lineItemUid; }
    public void setLineItemUid(String lineItemUid) { this.lineItemUid = lineItemUid; }

    public MerchantSettlementCycle getCycle() { return cycle; }
    public void setCycle(MerchantSettlementCycle cycle) { this.cycle = cycle; }

    public String getTxnReference() { return txnReference; }
    public void setTxnReference(String txnReference) { this.txnReference = txnReference; }

    public long getPointsAmount() { return pointsAmount; }
    public void setPointsAmount(long pointsAmount) { this.pointsAmount = pointsAmount; }

    public BigDecimal getMonetaryValue() { return monetaryValue; }
    public void setMonetaryValue(BigDecimal monetaryValue) { this.monetaryValue = monetaryValue; }

    public boolean isDisputed() { return disputed; }
    public void setDisputed(boolean disputed) { this.disputed = disputed; }

    public Instant getCreatedAt() { return createdAt; }
}
