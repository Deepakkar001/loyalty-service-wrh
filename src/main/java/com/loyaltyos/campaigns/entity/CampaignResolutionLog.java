package com.loyaltyos.campaigns.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "campaign_resolution_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tenant_event_resolution",
        columnNames = {"tenant_id", "event_id"}
    ),
    indexes = {
        @Index(name = "idx_tenant_customer", columnList = "tenant_id,customer_id"),
        @Index(name = "idx_tenant_resolved", columnList = "tenant_id,resolved_at")
    }
)
public class CampaignResolutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campaigns_evaluated", columnDefinition = "JSON")
    private JsonNode campaignsEvaluated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campaigns_applied", columnDefinition = "JSON")
    private JsonNode campaignsApplied;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campaigns_dropped", columnDefinition = "JSON")
    private JsonNode campaignsDropped;

    @Column(name = "total_points_awarded", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalPointsAwarded = BigDecimal.ZERO;

    @Column(name = "resolution_mode", length = 64)
    private String resolutionMode;

    @Column(name = "cap_applied", nullable = false)
    private boolean capApplied;

    @CreationTimestamp
    @Column(name = "resolved_at", nullable = false, updatable = false)
    private Instant resolvedAt;

    public CampaignResolutionLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public JsonNode getCampaignsEvaluated() { return campaignsEvaluated; }
    public void setCampaignsEvaluated(JsonNode campaignsEvaluated) { this.campaignsEvaluated = campaignsEvaluated; }
    public JsonNode getCampaignsApplied() { return campaignsApplied; }
    public void setCampaignsApplied(JsonNode campaignsApplied) { this.campaignsApplied = campaignsApplied; }
    public JsonNode getCampaignsDropped() { return campaignsDropped; }
    public void setCampaignsDropped(JsonNode campaignsDropped) { this.campaignsDropped = campaignsDropped; }
    public BigDecimal getTotalPointsAwarded() { return totalPointsAwarded; }
    public void setTotalPointsAwarded(BigDecimal totalPointsAwarded) { this.totalPointsAwarded = totalPointsAwarded; }
    public String getResolutionMode() { return resolutionMode; }
    public void setResolutionMode(String resolutionMode) { this.resolutionMode = resolutionMode; }
    public boolean isCapApplied() { return capApplied; }
    public void setCapApplied(boolean capApplied) { this.capApplied = capApplied; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
