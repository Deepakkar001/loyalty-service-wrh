package com.loyaltyos.referrals.dto;

import java.time.Instant;
import java.util.List;

public class ReferralFraudQueueItemResponse {

    private String referralUid;
    private String referrerCustomerId;
    private String refereeCustomerId;
    private String referralCodeUsed;
    private List<String> fraudReasons;
    private Instant createdAt;

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

    public String getReferralCodeUsed() {
        return referralCodeUsed;
    }

    public void setReferralCodeUsed(String referralCodeUsed) {
        this.referralCodeUsed = referralCodeUsed;
    }

    public List<String> getFraudReasons() {
        return fraudReasons;
    }

    public void setFraudReasons(List<String> fraudReasons) {
        this.fraudReasons = fraudReasons;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
