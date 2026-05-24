package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EventProcessingResponse {

    private String status = "SUCCESS";
    private String eventId;
    private Instant timestamp;
    private CustomerInfo customer;
    private EarningsInfo earnings;
    private RulesInfo rules;
    private CampaignsInfo campaigns;
    private String idempotencyKey;
    private Integer processingTimeMs;

    public static class CustomerInfo {
        private String customerId;
        private String tierBefore;
        private String tierAfter;
        private boolean tierChanged;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getTierBefore() { return tierBefore; }
        public void setTierBefore(String tierBefore) { this.tierBefore = tierBefore; }
        public String getTierAfter() { return tierAfter; }
        public void setTierAfter(String tierAfter) { this.tierAfter = tierAfter; }
        public boolean isTierChanged() { return tierChanged; }
        public void setTierChanged(boolean tierChanged) { this.tierChanged = tierChanged; }
    }

    public static class EarningsInfo {
        private BigDecimal basePoints;
        private BigDecimal tierMultiplier;
        private BigDecimal finalPoints;
        private BigDecimal previousBalance;
        private BigDecimal newBalance;
        private Instant pointsExpireAt;

        public BigDecimal getBasePoints() { return basePoints; }
        public void setBasePoints(BigDecimal basePoints) { this.basePoints = basePoints; }
        public BigDecimal getTierMultiplier() { return tierMultiplier; }
        public void setTierMultiplier(BigDecimal tierMultiplier) { this.tierMultiplier = tierMultiplier; }
        public BigDecimal getFinalPoints() { return finalPoints; }
        public void setFinalPoints(BigDecimal finalPoints) { this.finalPoints = finalPoints; }
        public BigDecimal getPreviousBalance() { return previousBalance; }
        public void setPreviousBalance(BigDecimal previousBalance) { this.previousBalance = previousBalance; }
        public BigDecimal getNewBalance() { return newBalance; }
        public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
        public Instant getPointsExpireAt() { return pointsExpireAt; }
        public void setPointsExpireAt(Instant pointsExpireAt) { this.pointsExpireAt = pointsExpireAt; }
    }

    public static class MatchedRuleLine {
        private String ruleId;
        private String ruleName;
        private Integer priority;
        private BigDecimal pointsAwarded;
        private BigDecimal multiplier;

        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public BigDecimal getPointsAwarded() { return pointsAwarded; }
        public void setPointsAwarded(BigDecimal pointsAwarded) { this.pointsAwarded = pointsAwarded; }
        public BigDecimal getMultiplier() { return multiplier; }
        public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
    }

    public static class SuppressedRuleLine {
        private String ruleId;
        private String ruleName;
        private String reason;

        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RulesInfo {
        private List<MatchedRuleLine> matched = new ArrayList<>();
        private List<SuppressedRuleLine> suppressed = new ArrayList<>();

        public List<MatchedRuleLine> getMatched() { return matched; }
        public void setMatched(List<MatchedRuleLine> matched) { this.matched = matched; }
        public List<SuppressedRuleLine> getSuppressed() { return suppressed; }
        public void setSuppressed(List<SuppressedRuleLine> suppressed) { this.suppressed = suppressed; }
    }

    public static class EligibleCampaignLine {
        private String campaignId;
        private String campaignName;
        private BigDecimal bonusPoints;
        private Instant endsAt;

        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
        public BigDecimal getBonusPoints() { return bonusPoints; }
        public void setBonusPoints(BigDecimal bonusPoints) { this.bonusPoints = bonusPoints; }
        public Instant getEndsAt() { return endsAt; }
        public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    }

    public static class EnrolledCampaignLine {
        private String campaignId;
        private String campaignName;
        private String status;

        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class CampaignsInfo {
        private List<EligibleCampaignLine> eligible = new ArrayList<>();
        private List<EnrolledCampaignLine> enrolled = new ArrayList<>();

        public List<EligibleCampaignLine> getEligible() { return eligible; }
        public void setEligible(List<EligibleCampaignLine> eligible) { this.eligible = eligible; }
        public List<EnrolledCampaignLine> getEnrolled() { return enrolled; }
        public void setEnrolled(List<EnrolledCampaignLine> enrolled) { this.enrolled = enrolled; }
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public CustomerInfo getCustomer() { return customer; }
    public void setCustomer(CustomerInfo customer) { this.customer = customer; }
    public EarningsInfo getEarnings() { return earnings; }
    public void setEarnings(EarningsInfo earnings) { this.earnings = earnings; }
    public RulesInfo getRules() { return rules; }
    public void setRules(RulesInfo rules) { this.rules = rules; }
    public CampaignsInfo getCampaigns() { return campaigns; }
    public void setCampaigns(CampaignsInfo campaigns) { this.campaigns = campaigns; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Integer getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
