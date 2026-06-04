package com.loyaltyos.referrals.dto;

public class ReferralTrendPointResponse {

    private String periodStart;
    private long referrals;
    private long rewarded;

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    public long getReferrals() {
        return referrals;
    }

    public void setReferrals(long referrals) {
        this.referrals = referrals;
    }

    public long getRewarded() {
        return rewarded;
    }

    public void setRewarded(long rewarded) {
        this.rewarded = rewarded;
    }
}
