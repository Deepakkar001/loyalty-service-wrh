package com.loyaltyos.referrals.model;

public class ReferralCapRule {

    public enum CapType {
        /** Uses referral_programmes.max_referrals_per_customer when maxCount omitted. */
        LIFETIME_REFERRALS,
        CALENDAR_MONTH_REFERRALS,
        ROLLING_DAY_REFERRALS
    }

    private CapType type = CapType.LIFETIME_REFERRALS;
    private Integer maxCount;
    /** For ROLLING_DAY_REFERRALS; default 30 if null. */
    private Integer windowDays;

    public CapType getType() {
        return type;
    }

    public void setType(CapType type) {
        this.type = type;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Integer getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(Integer windowDays) {
        this.windowDays = windowDays;
    }
}
