package com.loyaltyos.referrals.model;

public class ReferralFraudPolicy {

    private boolean blockSelfReferralByCustomerId = true;
    private int maxReferralsPer24Hours = 20;
    private boolean matchPhoneWhenProvided = true;
    private boolean matchEmailWhenProvided = true;
    private boolean matchDeviceWhenProvided = true;

    public boolean isBlockSelfReferralByCustomerId() {
        return blockSelfReferralByCustomerId;
    }

    public void setBlockSelfReferralByCustomerId(boolean blockSelfReferralByCustomerId) {
        this.blockSelfReferralByCustomerId = blockSelfReferralByCustomerId;
    }

    public int getMaxReferralsPer24Hours() {
        return maxReferralsPer24Hours;
    }

    public void setMaxReferralsPer24Hours(int maxReferralsPer24Hours) {
        this.maxReferralsPer24Hours = maxReferralsPer24Hours;
    }

    public boolean isMatchPhoneWhenProvided() {
        return matchPhoneWhenProvided;
    }

    public void setMatchPhoneWhenProvided(boolean matchPhoneWhenProvided) {
        this.matchPhoneWhenProvided = matchPhoneWhenProvided;
    }

    public boolean isMatchEmailWhenProvided() {
        return matchEmailWhenProvided;
    }

    public void setMatchEmailWhenProvided(boolean matchEmailWhenProvided) {
        this.matchEmailWhenProvided = matchEmailWhenProvided;
    }

    public boolean isMatchDeviceWhenProvided() {
        return matchDeviceWhenProvided;
    }

    public void setMatchDeviceWhenProvided(boolean matchDeviceWhenProvided) {
        this.matchDeviceWhenProvided = matchDeviceWhenProvided;
    }
}
