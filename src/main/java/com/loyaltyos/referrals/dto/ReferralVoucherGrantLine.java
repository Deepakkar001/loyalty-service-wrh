package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public class ReferralVoucherGrantLine {

    private String customerId;
    private String catalogRewardUid;
    private String redemptionId;
    private BigDecimal pointsToRedeem;
    private BigDecimal faceValue;
    private BigDecimal fundingPoints;
    private int stage;
    private String recipientType;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCatalogRewardUid() {
        return catalogRewardUid;
    }

    public void setCatalogRewardUid(String catalogRewardUid) {
        this.catalogRewardUid = catalogRewardUid;
    }

    public String getRedemptionId() {
        return redemptionId;
    }

    public void setRedemptionId(String redemptionId) {
        this.redemptionId = redemptionId;
    }

    public BigDecimal getPointsToRedeem() {
        return pointsToRedeem;
    }

    public void setPointsToRedeem(BigDecimal pointsToRedeem) {
        this.pointsToRedeem = pointsToRedeem;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }

    public BigDecimal getFundingPoints() {
        return fundingPoints;
    }

    public void setFundingPoints(BigDecimal fundingPoints) {
        this.fundingPoints = fundingPoints;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }
}
