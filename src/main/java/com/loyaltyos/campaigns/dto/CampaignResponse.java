package com.loyaltyos.campaigns.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.StackMode;
import java.math.BigDecimal;
import java.time.Instant;

public class CampaignResponse {

    private String tenantId;
    private String programmeUid;
    private String campaignUid;
    private String name;
    private String description;
    private String campaignType;
    private JsonNode occasionTags;
    private CampaignStatus status;
    private JsonNode targetSegment;
    private JsonNode eligibilityRules;
    private String triggerEventType;
    private JsonNode eventSchema;
    private JsonNode offerConfig;
    private String mutualExclGroup;
    private StackMode stackMode;
    private BigDecimal budgetTotal;
    private BigDecimal budgetConsumed;
    private BigDecimal budgetConsumedPct;
    private BigDecimal budgetRemaining;
    private BigDecimal alertThresholdPct;
    private Integer priority;
    private Integer maxParticipations;
    private Integer maxPerCustomer;
    private BigDecimal globalRewardCap;
    private String merchantId;
    private Instant validFrom;
    private Instant validUntil;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean budgetExceedsApprovalThreshold;

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
    public JsonNode getEventSchema() { return eventSchema; }
    public void setEventSchema(JsonNode eventSchema) { this.eventSchema = eventSchema; }
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
    public BigDecimal getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(BigDecimal budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }
    public BigDecimal getBudgetRemaining() { return budgetRemaining; }
    public void setBudgetRemaining(BigDecimal budgetRemaining) { this.budgetRemaining = budgetRemaining; }
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
    public boolean isBudgetExceedsApprovalThreshold() { return budgetExceedsApprovalThreshold; }
    public void setBudgetExceedsApprovalThreshold(boolean budgetExceedsApprovalThreshold) {
        this.budgetExceedsApprovalThreshold = budgetExceedsApprovalThreshold;
    }
}
