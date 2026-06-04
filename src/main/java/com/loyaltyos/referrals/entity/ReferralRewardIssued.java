package com.loyaltyos.referrals.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "referral_rewards_issued")
public class ReferralRewardIssued {

    public enum RecipientType {
        REFERRER,
        REFEREE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "referral_uid", nullable = false, length = 128)
    private String referralUid;

    @Column(name = "stage", nullable = false)
    private int stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 16)
    private RecipientType recipientType;

    @Column(name = "reward_type", nullable = false, length = 16)
    private String rewardType = "POINTS";

    @Column(name = "catalog_reward_uid", length = 64)
    private String catalogRewardUid;

    @Column(name = "recipient_customer_id", nullable = false, length = 128)
    private String recipientCustomerId;

    @Column(name = "points_awarded", nullable = false, precision = 18, scale = 4)
    private BigDecimal pointsAwarded;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getReferralUid() { return referralUid; }
    public void setReferralUid(String referralUid) { this.referralUid = referralUid; }
    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
    public RecipientType getRecipientType() { return recipientType; }
    public void setRecipientType(RecipientType recipientType) { this.recipientType = recipientType; }
    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public String getRecipientCustomerId() { return recipientCustomerId; }
    public void setRecipientCustomerId(String recipientCustomerId) { this.recipientCustomerId = recipientCustomerId; }
    public BigDecimal getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(BigDecimal pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

