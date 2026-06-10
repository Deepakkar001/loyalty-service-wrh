package com.loyaltyos.campaigns.dto;

import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import java.math.BigDecimal;
import java.time.Instant;

public class CampaignPerformanceRow {

    private String campaignUid;
    private String campaignName;
    private CampaignStatus status;
    private CustomerScope customerScope;
    private String awardType;
    private Instant validFrom;
    private Instant validUntil;
    private BigDecimal budgetTotal;
    private BigDecimal budgetConsumed;
    private BigDecimal budgetConsumedPct;
    private BigDecimal budgetRemaining;
    private Integer maxParticipations;
    private Integer maxPerCustomer;
    private int targetAudienceSize;
    private long participationsInPeriod;
    private long uniqueCustomersInPeriod;
    private BigDecimal pointsInPeriod;
    private BigDecimal cashbackInPeriod;
    private long participationsAllTime;
    private long uniqueCustomersAllTime;
    private BigDecimal avgPointsPerParticipation;
    private BigDecimal avgCashbackPerParticipation;
    private BigDecimal avgParticipationsPerCustomer;
    private BigDecimal audienceReachPct;
    private BigDecimal participationCapPct;
    private BigDecimal rewardCostPerParticipation;
    private BigDecimal periodOverPeriodChangePct;
    private Instant firstParticipationAt;
    private Instant lastParticipationAt;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public CustomerScope getCustomerScope() { return customerScope; }
    public void setCustomerScope(CustomerScope customerScope) { this.customerScope = customerScope; }
    public String getAwardType() { return awardType; }
    public void setAwardType(String awardType) { this.awardType = awardType; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
    public BigDecimal getBudgetConsumed() { return budgetConsumed; }
    public void setBudgetConsumed(BigDecimal budgetConsumed) { this.budgetConsumed = budgetConsumed; }
    public BigDecimal getBudgetConsumedPct() { return budgetConsumedPct; }
    public void setBudgetConsumedPct(BigDecimal budgetConsumedPct) { this.budgetConsumedPct = budgetConsumedPct; }
    public BigDecimal getBudgetRemaining() { return budgetRemaining; }
    public void setBudgetRemaining(BigDecimal budgetRemaining) { this.budgetRemaining = budgetRemaining; }
    public Integer getMaxParticipations() { return maxParticipations; }
    public void setMaxParticipations(Integer maxParticipations) { this.maxParticipations = maxParticipations; }
    public Integer getMaxPerCustomer() { return maxPerCustomer; }
    public void setMaxPerCustomer(Integer maxPerCustomer) { this.maxPerCustomer = maxPerCustomer; }
    public int getTargetAudienceSize() { return targetAudienceSize; }
    public void setTargetAudienceSize(int targetAudienceSize) { this.targetAudienceSize = targetAudienceSize; }
    public long getParticipationsInPeriod() { return participationsInPeriod; }
    public void setParticipationsInPeriod(long participationsInPeriod) { this.participationsInPeriod = participationsInPeriod; }
    public long getUniqueCustomersInPeriod() { return uniqueCustomersInPeriod; }
    public void setUniqueCustomersInPeriod(long uniqueCustomersInPeriod) { this.uniqueCustomersInPeriod = uniqueCustomersInPeriod; }
    public BigDecimal getPointsInPeriod() { return pointsInPeriod; }
    public void setPointsInPeriod(BigDecimal pointsInPeriod) { this.pointsInPeriod = pointsInPeriod; }
    public BigDecimal getCashbackInPeriod() { return cashbackInPeriod; }
    public void setCashbackInPeriod(BigDecimal cashbackInPeriod) { this.cashbackInPeriod = cashbackInPeriod; }
    public long getParticipationsAllTime() { return participationsAllTime; }
    public void setParticipationsAllTime(long participationsAllTime) { this.participationsAllTime = participationsAllTime; }
    public long getUniqueCustomersAllTime() { return uniqueCustomersAllTime; }
    public void setUniqueCustomersAllTime(long uniqueCustomersAllTime) { this.uniqueCustomersAllTime = uniqueCustomersAllTime; }
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
    public BigDecimal getPeriodOverPeriodChangePct() { return periodOverPeriodChangePct; }
    public void setPeriodOverPeriodChangePct(BigDecimal periodOverPeriodChangePct) { this.periodOverPeriodChangePct = periodOverPeriodChangePct; }
    public Instant getFirstParticipationAt() { return firstParticipationAt; }
    public void setFirstParticipationAt(Instant firstParticipationAt) { this.firstParticipationAt = firstParticipationAt; }
    public Instant getLastParticipationAt() { return lastParticipationAt; }
    public void setLastParticipationAt(Instant lastParticipationAt) { this.lastParticipationAt = lastParticipationAt; }
}
