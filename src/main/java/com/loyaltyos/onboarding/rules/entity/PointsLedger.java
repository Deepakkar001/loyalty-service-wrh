package com.loyaltyos.onboarding.rules.entity;

import com.loyaltyos.onboarding.rules.enums.LedgerEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "points_ledger",
    indexes = {
        @Index(name = "idx_tenant_customer", columnList = "tenant_id,customer_id"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
    }
)
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 32)
    private LedgerEntryType entryType;

    @Column(name = "points", nullable = false, precision = 18, scale = 4)
    private BigDecimal points;

    @Column(name = "source_rule_id")
    private Long sourceRuleId;

    @Column(name = "source_event_id", length = 128)
    private String sourceEventId;

    @Column(name = "source_campaign_id", length = 128)
    private String sourceCampaignId;

    @Column(name = "reversal_of_ledger_id")
    private Long reversalOfLedgerId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public PointsLedger() {}

    public PointsLedger(
        Long id,
        String tenantId,
        String customerId,
        String programmeUid,
        String idempotencyKey,
        LedgerEntryType entryType,
        BigDecimal points,
        Long sourceRuleId,
        String sourceEventId,
        String sourceCampaignId,
        Long reversalOfLedgerId,
        Instant expiresAt,
        String description,
        String createdBy,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.programmeUid = (programmeUid == null || programmeUid.isBlank()) ? "default" : programmeUid;
        this.idempotencyKey = idempotencyKey;
        this.entryType = entryType;
        this.points = points;
        this.sourceRuleId = sourceRuleId;
        this.sourceEventId = sourceEventId;
        this.sourceCampaignId = sourceCampaignId;
        this.reversalOfLedgerId = reversalOfLedgerId;
        this.expiresAt = expiresAt;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String customerId;
        private String programmeUid = "default";
        private String idempotencyKey;
        private LedgerEntryType entryType;
        private BigDecimal points;
        private Long sourceRuleId;
        private String sourceEventId;
        private String sourceCampaignId;
        private Long reversalOfLedgerId;
        private Instant expiresAt;
        private String description;
        private String createdBy;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder entryType(LedgerEntryType entryType) { this.entryType = entryType; return this; }
        public Builder points(BigDecimal points) { this.points = points; return this; }
        public Builder sourceRuleId(Long sourceRuleId) { this.sourceRuleId = sourceRuleId; return this; }
        public Builder sourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; return this; }
        public Builder sourceCampaignId(String sourceCampaignId) { this.sourceCampaignId = sourceCampaignId; return this; }
        public Builder reversalOfLedgerId(Long reversalOfLedgerId) { this.reversalOfLedgerId = reversalOfLedgerId; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public PointsLedger build() {
            return new PointsLedger(
                id, tenantId, customerId, programmeUid, idempotencyKey, entryType, points,
                sourceRuleId, sourceEventId, sourceCampaignId, reversalOfLedgerId, expiresAt, description, createdBy, createdAt
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LedgerEntryType getEntryType() { return entryType; }
    public void setEntryType(LedgerEntryType entryType) { this.entryType = entryType; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public Long getSourceRuleId() { return sourceRuleId; }
    public void setSourceRuleId(Long sourceRuleId) { this.sourceRuleId = sourceRuleId; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
    public String getSourceCampaignId() { return sourceCampaignId; }
    public void setSourceCampaignId(String sourceCampaignId) { this.sourceCampaignId = sourceCampaignId; }
    public Long getReversalOfLedgerId() { return reversalOfLedgerId; }
    public void setReversalOfLedgerId(Long reversalOfLedgerId) { this.reversalOfLedgerId = reversalOfLedgerId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
