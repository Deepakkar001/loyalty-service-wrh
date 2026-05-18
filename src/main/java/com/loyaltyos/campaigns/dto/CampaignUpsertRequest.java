package com.loyaltyos.campaigns.dto;

import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CampaignUpsertRequest {

    @NotBlank
    @Size(max = 64)
    private String programmeUid;

    @Size(max = 128)
    private String campaignUid;

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank
    @Size(max = 64)
    private String campaignType;

    private List<String> occasionTags;

    @Valid
    private CampaignTargetSegment targetSegment;

    @NotNull
    @Valid
    private CampaignOfferConfig offerConfig;

    private String triggerEventType;

    @Size(max = 64)
    private String mutualExclGroup;

    private String stackMode;

    @NotNull
    @Positive
    private BigDecimal budgetTotal;

    private BigDecimal alertThresholdPct;

    private Integer priority;

    private Integer maxParticipations;

    private Integer maxPerCustomer;

    private BigDecimal globalRewardCap;

    @Size(max = 128)
    private String merchantId;

    @NotNull
    private Instant validFrom;

    @NotNull
    private Instant validUntil;

    public CampaignUpsertRequest() {}

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
    public List<String> getOccasionTags() { return occasionTags; }
    public void setOccasionTags(List<String> occasionTags) { this.occasionTags = occasionTags; }
    public CampaignTargetSegment getTargetSegment() { return targetSegment; }
    public void setTargetSegment(CampaignTargetSegment targetSegment) { this.targetSegment = targetSegment; }
    public CampaignOfferConfig getOfferConfig() { return offerConfig; }
    public void setOfferConfig(CampaignOfferConfig offerConfig) { this.offerConfig = offerConfig; }
    public String getTriggerEventType() { return triggerEventType; }
    public void setTriggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; }
    public String getMutualExclGroup() { return mutualExclGroup; }
    public void setMutualExclGroup(String mutualExclGroup) { this.mutualExclGroup = mutualExclGroup; }
    public String getStackMode() { return stackMode; }
    public void setStackMode(String stackMode) { this.stackMode = stackMode; }
    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
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
}
