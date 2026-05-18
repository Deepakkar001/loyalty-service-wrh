package com.loyaltyos.campaigns.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.StackMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "campaigns",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tenant_campaign",
        columnNames = {"tenant_id", "campaign_uid"}
    ),
    indexes = {
        @Index(name = "idx_tenant_prog_status_window", columnList = "tenant_id,programme_uid,status,valid_from,valid_until"),
        @Index(name = "idx_tenant_prog_priority", columnList = "tenant_id,programme_uid,priority")
    }
)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "campaign_uid", nullable = false, length = 128)
    private String campaignUid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "campaign_type", nullable = false, length = 64)
    private String campaignType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "occasion_tags", columnDefinition = "JSON")
    private JsonNode occasionTags;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_segment", columnDefinition = "JSON")
    private JsonNode targetSegment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_rules", columnDefinition = "JSON")
    private JsonNode eligibilityRules;

    @Column(name = "trigger_event_type", length = 64)
    private String triggerEventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offer_config", nullable = false, columnDefinition = "JSON")
    private JsonNode offerConfig;

    @Column(name = "mutual_excl_group", length = 64)
    private String mutualExclGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "stack_mode", nullable = false, length = 32)
    private StackMode stackMode = StackMode.ADDITIVE;

    @Column(name = "budget_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetTotal;

    @Column(name = "budget_consumed", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetConsumed = BigDecimal.ZERO;

    @Column(name = "alert_threshold_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal alertThresholdPct = new BigDecimal("80.00");

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "max_participations")
    private Integer maxParticipations;

    @Column(name = "max_per_customer")
    private Integer maxPerCustomer;

    @Column(name = "global_reward_cap", precision = 18, scale = 4)
    private BigDecimal globalRewardCap;

    @Column(name = "merchant_id", length = 128)
    private String merchantId;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Campaign() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCampaignType() { return campaignType; }
    public void setCampaignType(String campaignType) { this.campaignType = campaignType; }
    public JsonNode getOccasionTags() { return occasionTags; }
    public void setOccasionTags(JsonNode occasionTags) { this.occasionTags = occasionTags; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public JsonNode getTargetSegment() { return targetSegment; }
    public void setTargetSegment(JsonNode targetSegment) { this.targetSegment = targetSegment; }
    public JsonNode getEligibilityRules() { return eligibilityRules; }
    public void setEligibilityRules(JsonNode eligibilityRules) { this.eligibilityRules = eligibilityRules; }
    public String getTriggerEventType() { return triggerEventType; }
    public void setTriggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; }
    public JsonNode getOfferConfig() { return offerConfig; }
    public void setOfferConfig(JsonNode offerConfig) { this.offerConfig = offerConfig; }
    public String getMutualExclGroup() { return mutualExclGroup; }
    public void setMutualExclGroup(String mutualExclGroup) { this.mutualExclGroup = mutualExclGroup; }
    public StackMode getStackMode() { return stackMode; }
    public void setStackMode(StackMode stackMode) { this.stackMode = stackMode; }
    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }
    public BigDecimal getAlertThresholdPct() { return alertThresholdPct; }
    public void setAlertThresholdPct(BigDecimal alertThresholdPct) { this.alertThresholdPct = alertThresholdPct; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getMaxParticipations() { return maxParticipations; }
    public void setMaxParticipations(Integer maxParticipations) { this.maxParticipations = maxParticipations; }
    public Integer getMaxPerCustomer() { return maxPerCustomer; }
    public void setMaxPerCustomer(Integer maxPerCustomer) { this.maxPerCustomer = maxPerCustomer; }
    public BigDecimal getGlobalRewardCap() { return globalRewardCap; }
    public void setGlobalRewardCap(BigDecimal globalRewardCap) { this.globalRewardCap = globalRewardCap; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
