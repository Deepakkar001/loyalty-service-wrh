package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReferralEffectivenessReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String currency;
    private ReferralPeriodMetrics periodMetrics;
    private ReferralPeriodMetrics priorPeriodMetrics;
    private BigDecimal periodOverPeriodReferralsChangePct;
    private BigDecimal periodOverPeriodRewardedChangePct;
    private BigDecimal periodOverPeriodConversionChangePts;
    private BigDecimal rewardCostInCurrency;
    private BigDecimal revenuePerRewardCurrency;
    private BigDecimal netRefereeValue;
    private BigDecimal avgPointsPerRewardedReferral;
    private BigDecimal costPerRewardedReferralPoints;
    private ReferralTimeToPurchaseResponse timeToFirstPurchase;
    private List<ReferralFunnelStageRow> funnel = new ArrayList<>();
    private List<ReferralEffectivenessTrendRow> dailyTrends = new ArrayList<>();
    private List<ReferralTopReferrerResponse> topReferrers = new ArrayList<>();
    private List<ReferralProgrammeComparisonRow> programmeComparisons = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public ReferralPeriodMetrics getPeriodMetrics() { return periodMetrics; }
    public void setPeriodMetrics(ReferralPeriodMetrics periodMetrics) { this.periodMetrics = periodMetrics; }
    public ReferralPeriodMetrics getPriorPeriodMetrics() { return priorPeriodMetrics; }
    public void setPriorPeriodMetrics(ReferralPeriodMetrics priorPeriodMetrics) { this.priorPeriodMetrics = priorPeriodMetrics; }
    public BigDecimal getPeriodOverPeriodReferralsChangePct() { return periodOverPeriodReferralsChangePct; }
    public void setPeriodOverPeriodReferralsChangePct(BigDecimal periodOverPeriodReferralsChangePct) { this.periodOverPeriodReferralsChangePct = periodOverPeriodReferralsChangePct; }
    public BigDecimal getPeriodOverPeriodRewardedChangePct() { return periodOverPeriodRewardedChangePct; }
    public void setPeriodOverPeriodRewardedChangePct(BigDecimal periodOverPeriodRewardedChangePct) { this.periodOverPeriodRewardedChangePct = periodOverPeriodRewardedChangePct; }
    public BigDecimal getPeriodOverPeriodConversionChangePts() { return periodOverPeriodConversionChangePts; }
    public void setPeriodOverPeriodConversionChangePts(BigDecimal periodOverPeriodConversionChangePts) { this.periodOverPeriodConversionChangePts = periodOverPeriodConversionChangePts; }
    public BigDecimal getRewardCostInCurrency() { return rewardCostInCurrency; }
    public void setRewardCostInCurrency(BigDecimal rewardCostInCurrency) { this.rewardCostInCurrency = rewardCostInCurrency; }
    public BigDecimal getRevenuePerRewardCurrency() { return revenuePerRewardCurrency; }
    public void setRevenuePerRewardCurrency(BigDecimal revenuePerRewardCurrency) { this.revenuePerRewardCurrency = revenuePerRewardCurrency; }
    public BigDecimal getNetRefereeValue() { return netRefereeValue; }
    public void setNetRefereeValue(BigDecimal netRefereeValue) { this.netRefereeValue = netRefereeValue; }
    public BigDecimal getAvgPointsPerRewardedReferral() { return avgPointsPerRewardedReferral; }
    public void setAvgPointsPerRewardedReferral(BigDecimal avgPointsPerRewardedReferral) { this.avgPointsPerRewardedReferral = avgPointsPerRewardedReferral; }
    public BigDecimal getCostPerRewardedReferralPoints() { return costPerRewardedReferralPoints; }
    public void setCostPerRewardedReferralPoints(BigDecimal costPerRewardedReferralPoints) { this.costPerRewardedReferralPoints = costPerRewardedReferralPoints; }
    public ReferralTimeToPurchaseResponse getTimeToFirstPurchase() { return timeToFirstPurchase; }
    public void setTimeToFirstPurchase(ReferralTimeToPurchaseResponse timeToFirstPurchase) { this.timeToFirstPurchase = timeToFirstPurchase; }
    public List<ReferralFunnelStageRow> getFunnel() { return funnel; }
    public void setFunnel(List<ReferralFunnelStageRow> funnel) { this.funnel = funnel; }
    public List<ReferralEffectivenessTrendRow> getDailyTrends() { return dailyTrends; }
    public void setDailyTrends(List<ReferralEffectivenessTrendRow> dailyTrends) { this.dailyTrends = dailyTrends; }
    public List<ReferralTopReferrerResponse> getTopReferrers() { return topReferrers; }
    public void setTopReferrers(List<ReferralTopReferrerResponse> topReferrers) { this.topReferrers = topReferrers; }
    public List<ReferralProgrammeComparisonRow> getProgrammeComparisons() { return programmeComparisons; }
    public void setProgrammeComparisons(List<ReferralProgrammeComparisonRow> programmeComparisons) { this.programmeComparisons = programmeComparisons; }
}
