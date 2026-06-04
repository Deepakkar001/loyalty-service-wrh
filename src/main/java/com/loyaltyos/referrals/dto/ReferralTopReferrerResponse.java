package com.loyaltyos.referrals.dto;

public class ReferralTopReferrerResponse {

    private String referrerCustomerId;
    private long referralCount;
    private long rewardedCount;

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
}
