package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class ReversalAdjustmentLedgerRow {

    private Long ledgerId;
    private String entryType;
    private String programmeUid;
    private String customerId;
    private BigDecimal points;
    private BigDecimal signedImpact;
    private Long reversalOfLedgerId;
    private String originalEntryType;
    private BigDecimal originalPoints;
    private Instant originalCreatedAt;
    private String sourceEventId;
    private String ruleName;
    private String description;
    private String createdBy;
    private Instant createdAt;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public BigDecimal getSignedImpact() { return signedImpact; }
    public void setSignedImpact(BigDecimal signedImpact) { this.signedImpact = signedImpact; }
    public Long getReversalOfLedgerId() { return reversalOfLedgerId; }
    public void setReversalOfLedgerId(Long reversalOfLedgerId) { this.reversalOfLedgerId = reversalOfLedgerId; }
    public String getOriginalEntryType() { return originalEntryType; }
    public void setOriginalEntryType(String originalEntryType) { this.originalEntryType = originalEntryType; }
    public BigDecimal getOriginalPoints() { return originalPoints; }
    public void setOriginalPoints(BigDecimal originalPoints) { this.originalPoints = originalPoints; }
    public Instant getOriginalCreatedAt() { return originalCreatedAt; }
    public void setOriginalCreatedAt(Instant originalCreatedAt) { this.originalCreatedAt = originalCreatedAt; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
