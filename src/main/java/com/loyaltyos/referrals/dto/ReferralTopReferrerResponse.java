package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public class ReferralTopReferrerResponse {

    private String referrerCustomerId;
    private long referralCount;
    private long rewardedCount;
    private BigDecimal totalRefereeSpend;
    private BigDecimal pointsEarned;
    private BigDecimal conversionRatePercent;

    public String getReferrerCustomerId() {
        return referrerCustomerId;
    }

    public void setReferrerCustomerId(String referrerCustomerId) {
        this.referrerCustomerId = referrerCustomerId;
    }

    public long getReferralCount() {
        return referralCount;
    }

    public void setReferralCount(long referralCount) {
        this.referralCount = referralCount;
    }

    public long getRewardedCount() {
        return rewardedCount;
    }

    public void setRewardedCount(long rewardedCount) {
        this.rewardedCount = rewardedCount;
    }

    public BigDecimal getTotalRefereeSpend() { return totalRefereeSpend; }
    public void setTotalRefereeSpend(BigDecimal totalRefereeSpend) { this.totalRefereeSpend = totalRefereeSpend; }
    public BigDecimal getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(BigDecimal pointsEarned) { this.pointsEarned = pointsEarned; }
    public BigDecimal getConversionRatePercent() { return conversionRatePercent; }
    public void setConversionRatePercent(BigDecimal conversionRatePercent) { this.conversionRatePercent = conversionRatePercent; }
}
