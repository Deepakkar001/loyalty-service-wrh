package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class MerchantSettlementCycleResponse {

    private String cycleUid;
    private String merchantUid;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private long totalPoints;
    private BigDecimal totalMonetaryValue;
    private Instant createdAt;
    private Instant finalizedAt;
    private int lineItemCount;
    private int openDisputeCount;

    public String getCycleUid() { return cycleUid; }
    public void setCycleUid(String cycleUid) { this.cycleUid = cycleUid; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTotalPoints() { return totalPoints; }
    public void setTotalPoints(long totalPoints) { this.totalPoints = totalPoints; }

    public BigDecimal getTotalMonetaryValue() { return totalMonetaryValue; }
    public void setTotalMonetaryValue(BigDecimal totalMonetaryValue) {
        this.totalMonetaryValue = totalMonetaryValue;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }

    public int getLineItemCount() { return lineItemCount; }
    public void setLineItemCount(int lineItemCount) { this.lineItemCount = lineItemCount; }

    public int getOpenDisputeCount() { return openDisputeCount; }
    public void setOpenDisputeCount(int openDisputeCount) { this.openDisputeCount = openDisputeCount; }
}
