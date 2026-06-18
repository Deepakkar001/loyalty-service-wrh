package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class SettlementLineItemResponse {

    private String lineItemUid;
    private String txnReference;
    private long pointsAmount;
    private BigDecimal monetaryValue;
    private boolean disputed;
    private Instant createdAt;

    public String getLineItemUid() { return lineItemUid; }
    public void setLineItemUid(String lineItemUid) { this.lineItemUid = lineItemUid; }

    public String getTxnReference() { return txnReference; }
    public void setTxnReference(String txnReference) { this.txnReference = txnReference; }

    public long getPointsAmount() { return pointsAmount; }
    public void setPointsAmount(long pointsAmount) { this.pointsAmount = pointsAmount; }

    public BigDecimal getMonetaryValue() { return monetaryValue; }
    public void setMonetaryValue(BigDecimal monetaryValue) { this.monetaryValue = monetaryValue; }

    public boolean isDisputed() { return disputed; }
    public void setDisputed(boolean disputed) { this.disputed = disputed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
