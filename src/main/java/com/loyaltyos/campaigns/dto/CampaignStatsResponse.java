package com.loyaltyos.campaigns.dto;

import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import java.math.BigDecimal;

public class CampaignStatsResponse {

    private String campaignUid;
    private String campaignName;
    private CampaignStatus status;
    private BigDecimal budgetTotal;
    private BigDecimal budgetConsumed;
    private BigDecimal budgetConsumedPct;
    private BigDecimal budgetRemaining;
    private long totalParticipations;
    private long uniqueCustomersReached;
    private BigDecimal totalPointsIssued;
    private BigDecimal totalCashbackRecorded;
    private CustomerScope customerScope;
    private String awardType;
    private int targetAudienceSize;
    private Integer maxParticipations;
    private Integer maxPerCustomer;
    private BigDecimal avgPointsPerParticipation;
    private BigDecimal avgCashbackPerParticipation;
    private BigDecimal avgParticipationsPerCustomer;
    private BigDecimal audienceReachPct;
    private BigDecimal participationCapPct;
    private BigDecimal rewardCostPerParticipation;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }
    public BigDecimal getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(BigDecimal budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }
    public BigDecimal getBudgetRemaining() { return budgetRemaining; }
    public void setBudgetRemaining(BigDecimal budgetRemaining) { this.budgetRemaining = budgetRemaining; }
    public long getTotalParticipations() { return totalParticipations; }
    public void setTotalParticipations(long totalParticipations) { this.totalParticipations = totalParticipations; }
    public long getUniqueCustomersReached() { return uniqueCustomersReached; }
    public void setUniqueCustomersReached(long uniqueCustomersReached) { this.uniqueCustomersReached = uniqueCustomersReached; }
    public BigDecimal getTotalPointsIssued() { return totalPointsIssued; }
    public void setTotalPointsIssued(BigDecimal totalPointsIssued) { this.totalPointsIssued = totalPointsIssued; }
    public BigDecimal getTotalCashbackRecorded() { return totalCashbackRecorded; }
    public void setTotalCashbackRecorded(BigDecimal totalCashbackRecorded) { this.totalCashbackRecorded = totalCashbackRecorded; }
    public CustomerScope getCustomerScope() { return customerScope; }
    public void setCustomerScope(CustomerScope customerScope) { this.customerScope = customerScope; }
    public String getAwardType() { return awardType; }
    public void setAwardType(String awardType) { this.awardType = awardType; }
    public int getTargetAudienceSize() { return targetAudienceSize; }
    public void setTargetAudienceSize(int targetAudienceSize) { this.targetAudienceSize = targetAudienceSize; }
    public Integer getMaxParticipations() { return maxParticipations; }
    public void setMaxParticipations(Integer maxParticipations) { this.maxParticipations = maxParticipations; }
    public Integer getMaxPerCustomer() { return maxPerCustomer; }
    public void setMaxPerCustomer(Integer maxPerCustomer) { this.maxPerCustomer = maxPerCustomer; }
    public BigDecimal getAvgPointsPerParticipation() { return avgPointsPerParticipation; }
    public void setAvgPointsPerParticipation(BigDecimal avgPointsPerParticipation) { this.avgPointsPerParticipation = avgPointsPerParticipation; }
    public BigDecimal getAvgCashbackPerParticipation() { return avgCashbackPerParticipation; }
    public void setAvgCashbackPerParticipation(BigDecimal avgCashbackPerParticipation) { this.avgCashbackPerParticipation = avgCashbackPerParticipation; }
    public BigDecimal getAvgParticipationsPerCustomer() { return avgParticipationsPerCustomer; }
    public void setAvgParticipationsPerCustomer(BigDecimal avgParticipationsPerCustomer) { this.avgParticipationsPerCustomer = avgParticipationsPerCustomer; }
    public BigDecimal getAudienceReachPct() { return audienceReachPct; }
    public void setAudienceReachPct(BigDecimal audienceReachPct) { this.audienceReachPct = audienceReachPct; }
    public BigDecimal getParticipationCapPct() { return participationCapPct; }
    public void setParticipationCapPct(BigDecimal participationCapPct) { this.participationCapPct = participationCapPct; }
    public BigDecimal getRewardCostPerParticipation() { return rewardCostPerParticipation; }
    public void setRewardCostPerParticipation(BigDecimal rewardCostPerParticipation) { this.rewardCostPerParticipation = rewardCostPerParticipation; }
}
