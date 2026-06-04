package com.loyaltyos.referrals.model;

import java.math.BigDecimal;

/**
 * Per-party reward on a stage (referrer or referee). Defaults to points.
 */
public class ReferralPartyRewardConfig {

    private ReferralRewardType type = ReferralRewardType.POINTS;
    private BigDecimal points = BigDecimal.ZERO;
    private String catalogRewardUid;
    private BigDecimal voucherFaceValue;
    private BigDecimal voucherPointsToRedeem;

    public ReferralRewardType getType() {
        return type != null ? type : ReferralRewardType.POINTS;
    }

    public void setType(ReferralRewardType type) {
        this.type = type;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public String getCatalogRewardUid() {
        return catalogRewardUid;
    }

    public void setCatalogRewardUid(String catalogRewardUid) {
        this.catalogRewardUid = catalogRewardUid;
    }

    public BigDecimal getVoucherFaceValue() {
        return voucherFaceValue;
    }

    public void setVoucherFaceValue(BigDecimal voucherFaceValue) {
        this.voucherFaceValue = voucherFaceValue;
    }

    public BigDecimal getVoucherPointsToRedeem() {
        return voucherPointsToRedeem;
    }

    public void setVoucherPointsToRedeem(BigDecimal voucherPointsToRedeem) {
        this.voucherPointsToRedeem = voucherPointsToRedeem;
    }
}
