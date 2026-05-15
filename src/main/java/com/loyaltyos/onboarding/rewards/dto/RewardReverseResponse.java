package com.loyaltyos.onboarding.rewards.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class RewardReverseResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    private Long creditLedgerId;
    private Long reversalLedgerId;
    private BigDecimal pointsReversed;
    private BigDecimal newBalance;
    private boolean idempotentReplay;
    private String message;
    private Instant processedAt;

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

    public Long getCreditLedgerId() {
        return creditLedgerId;
    }

    public void setCreditLedgerId(Long creditLedgerId) {
        this.creditLedgerId = creditLedgerId;
    }

    public Long getReversalLedgerId() {
        return reversalLedgerId;
    }

    public void setReversalLedgerId(Long reversalLedgerId) {
        this.reversalLedgerId = reversalLedgerId;
    }

    public BigDecimal getPointsReversed() {
        return pointsReversed;
    }

    public void setPointsReversed(BigDecimal pointsReversed) {
        this.pointsReversed = pointsReversed;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public boolean isIdempotentReplay() {
        return idempotentReplay;
    }

    public void setIdempotentReplay(boolean idempotentReplay) {
        this.idempotentReplay = idempotentReplay;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
