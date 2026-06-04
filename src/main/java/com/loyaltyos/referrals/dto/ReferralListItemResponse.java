package com.loyaltyos.referrals.dto;

import java.time.Instant;

public class ReferralListItemResponse {

    private String referralUid;
    private String referrerCustomerId;
    private String refereeCustomerId;
    private String status;
    private String referralCodeUsed;
    private int purchaseCount;
    private Instant createdAt;
    private Instant updatedAt;

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

    public String getReferralCodeUsed() {
        return referralCodeUsed;
    }

    public void setReferralCodeUsed(String referralCodeUsed) {
        this.referralCodeUsed = referralCodeUsed;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(int purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
