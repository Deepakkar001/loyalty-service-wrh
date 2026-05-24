package com.loyaltyos.integration.entity;

import com.loyaltyos.integration.enums.EventProcessingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "integration_event_processing_log",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "event_id"})
)
public class IntegrationEventProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "api_key_uid", length = 128)
    private String apiKeyUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private EventProcessingStatus processingStatus;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "rules_evaluated_count")
    private int rulesEvaluatedCount;

    @Column(name = "rules_matched_count")
    private int rulesMatchedCount;

    @Column(name = "base_points_calculated", precision = 12, scale = 2)
    private BigDecimal basePointsCalculated = BigDecimal.ZERO;

    @Column(name = "tier_multiplier", precision = 4, scale = 2)
    private BigDecimal tierMultiplier = BigDecimal.ONE;

    @Column(name = "total_points_awarded", precision = 12, scale = 2)
    private BigDecimal totalPointsAwarded = BigDecimal.ZERO;

    @Column(name = "previous_balance", precision = 12, scale = 2)
    private BigDecimal previousBalance = BigDecimal.ZERO;

    @Column(name = "new_balance", precision = 12, scale = 2)
    private BigDecimal newBalance = BigDecimal.ZERO;

    @Column(name = "tier_before", length = 50)
    private String tierBefore;

    @Column(name = "tier_after", length = 50)
    private String tierAfter;

    @Column(name = "tier_changed")
    private boolean tierChanged;

    @Column(name = "campaigns_eligible_count")
    private int campaignsEligibleCount;

    @Column(name = "campaign_bonus_points", precision = 12, scale = 2)
    private BigDecimal campaignBonusPoints = BigDecimal.ZERO;

    @Column(name = "processing_time_ms", nullable = false)
    private int processingTimeMs;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "request_payload_hash", nullable = false, length = 64)
    private String requestPayloadHash;

    @Lob
    @Column(name = "response_payload_json", columnDefinition = "MEDIUMTEXT")
    private String responsePayloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IntegrationEventProcessingLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getApiKeyUid() { return apiKeyUid; }
    public void setApiKeyUid(String apiKeyUid) { this.apiKeyUid = apiKeyUid; }
    public EventProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(EventProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    public int getRulesEvaluatedCount() { return rulesEvaluatedCount; }
    public void setRulesEvaluatedCount(int rulesEvaluatedCount) { this.rulesEvaluatedCount = rulesEvaluatedCount; }
    public int getRulesMatchedCount() { return rulesMatchedCount; }
    public void setRulesMatchedCount(int rulesMatchedCount) { this.rulesMatchedCount = rulesMatchedCount; }
    public BigDecimal getBasePointsCalculated() { return basePointsCalculated; }
    public void setBasePointsCalculated(BigDecimal basePointsCalculated) { this.basePointsCalculated = basePointsCalculated; }
    public BigDecimal getTierMultiplier() { return tierMultiplier; }
    public void setTierMultiplier(BigDecimal tierMultiplier) { this.tierMultiplier = tierMultiplier; }
    public BigDecimal getTotalPointsAwarded() { return totalPointsAwarded; }
    public void setTotalPointsAwarded(BigDecimal totalPointsAwarded) { this.totalPointsAwarded = totalPointsAwarded; }
    public BigDecimal getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(BigDecimal previousBalance) { this.previousBalance = previousBalance; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public String getTierBefore() { return tierBefore; }
    public void setTierBefore(String tierBefore) { this.tierBefore = tierBefore; }
    public String getTierAfter() { return tierAfter; }
    public void setTierAfter(String tierAfter) { this.tierAfter = tierAfter; }
    public boolean isTierChanged() { return tierChanged; }
    public void setTierChanged(boolean tierChanged) { this.tierChanged = tierChanged; }
    public int getCampaignsEligibleCount() { return campaignsEligibleCount; }
    public void setCampaignsEligibleCount(int campaignsEligibleCount) { this.campaignsEligibleCount = campaignsEligibleCount; }
    public BigDecimal getCampaignBonusPoints() { return campaignBonusPoints; }
    public void setCampaignBonusPoints(BigDecimal campaignBonusPoints) { this.campaignBonusPoints = campaignBonusPoints; }
    public int getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(int processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRequestPayloadHash() { return requestPayloadHash; }
    public void setRequestPayloadHash(String requestPayloadHash) { this.requestPayloadHash = requestPayloadHash; }
    public String getResponsePayloadJson() { return responsePayloadJson; }
    public void setResponsePayloadJson(String responsePayloadJson) { this.responsePayloadJson = responsePayloadJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
