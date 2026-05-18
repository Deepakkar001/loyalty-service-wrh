package com.loyaltyos.campaigns.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "campaign_participations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_participation",
        columnNames = {"tenant_id", "campaign_uid", "customer_id", "event_id"}
    ),
    indexes = {
        @Index(name = "idx_tenant_customer_campaign", columnList = "tenant_id,customer_id,campaign_uid"),
        @Index(name = "idx_tenant_programme", columnList = "tenant_id,programme_uid")
    }
)
public class CampaignParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "campaign_uid", nullable = false, length = 128)
    private String campaignUid;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "points_awarded", precision = 18, scale = 4)
    private BigDecimal pointsAwarded;

    @Column(name = "cashback_amount", precision = 18, scale = 4)
    private BigDecimal cashbackAmount;

    @CreationTimestamp
    @Column(name = "participated_at", nullable = false, updatable = false)
    private Instant participatedAt;

    public CampaignParticipation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BigDecimal getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(BigDecimal pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public BigDecimal getCashbackAmount() { return cashbackAmount; }
    public void setCashbackAmount(BigDecimal cashbackAmount) { this.cashbackAmount = cashbackAmount; }
    public Instant getParticipatedAt() { return participatedAt; }
    public void setParticipatedAt(Instant participatedAt) { this.participatedAt = participatedAt; }
}
