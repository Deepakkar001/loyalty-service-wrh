package com.loyaltyos.onboarding.rewards.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RewardIssueResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    private String eventId;

    private BigDecimal totalPointsIssued;
    private int ledgerRowsCreated;
    private BigDecimal newBalance;

    private boolean idempotentReplay;
    private String message;

    private Instant processedAt;
    private List<IssuedLedgerLine> ledgerLines = new ArrayList<>();

    public static class IssuedLedgerLine {
        private Long ledgerId;
        private String idempotencyKey;
        private BigDecimal points;
        private String sourceRuleUid;

        public IssuedLedgerLine() {}

        public IssuedLedgerLine(Long ledgerId, String idempotencyKey, BigDecimal points, String sourceRuleUid) {
            this.ledgerId = ledgerId;
            this.idempotencyKey = idempotencyKey;
            this.points = points;
            this.sourceRuleUid = sourceRuleUid;
        }

        public Long getLedgerId() {
            return ledgerId;
        }

        public void setLedgerId(Long ledgerId) {
            this.ledgerId = ledgerId;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public BigDecimal getPoints() {
            return points;
        }

        public void setPoints(BigDecimal points) {
            this.points = points;
        }

        public String getSourceRuleUid() {
            return sourceRuleUid;
        }

        public void setSourceRuleUid(String sourceRuleUid) {
            this.sourceRuleUid = sourceRuleUid;
        }
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public BigDecimal getTotalPointsIssued() {
        return totalPointsIssued;
    }

    public void setTotalPointsIssued(BigDecimal totalPointsIssued) {
        this.totalPointsIssued = totalPointsIssued;
    }

    public int getLedgerRowsCreated() {
        return ledgerRowsCreated;
    }

    public void setLedgerRowsCreated(int ledgerRowsCreated) {
        this.ledgerRowsCreated = ledgerRowsCreated;
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

    public List<IssuedLedgerLine> getLedgerLines() {
        return ledgerLines;
    }

    public void setLedgerLines(List<IssuedLedgerLine> ledgerLines) {
        this.ledgerLines = ledgerLines != null ? ledgerLines : new ArrayList<>();
    }
}
