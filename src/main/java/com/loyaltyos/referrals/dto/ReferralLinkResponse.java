package com.loyaltyos.referrals.dto;

public class ReferralLinkResponse {

    private String referralUid;
    private String referrerCustomerId;
    private String refereeCustomerId;
    private String status;
    private boolean idempotentReplay;

    public String getReferralUid() {
        return referralUid;
    }

    public void setReferralUid(String referralUid) {
        this.referralUid = referralUid;
    }

    public String getReferrerCustomerId() {
        return referrerCustomerId;
    }

    public void setReferrerCustomerId(String referrerCustomerId) {
        this.referrerCustomerId = referrerCustomerId;
    }

    public String getRefereeCustomerId() {
        return refereeCustomerId;
    }

    public void setRefereeCustomerId(String refereeCustomerId) {
        this.refereeCustomerId = refereeCustomerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isIdempotentReplay() {
        return idempotentReplay;
    }

    public void setIdempotentReplay(boolean idempotentReplay) {
        this.idempotentReplay = idempotentReplay;
    }
}
