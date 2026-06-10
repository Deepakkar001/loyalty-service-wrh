package com.loyaltyos.coupon.dto;

import java.math.BigDecimal;

public class CouponPerformanceRow {

    private String couponUid;
    private String couponCode;
    private String couponName;
    private String couponType;
    private String status;
    private int maxRedemptions;
    private long redemptionsInPeriod;
    private long redemptionsAllTime;
    private BigDecimal discountInPeriod;
    private BigDecimal utilizationPct;
    private BigDecimal avgDiscountPerRedemption;

    public String getCouponUid() { return couponUid; }
    public void setCouponUid(String couponUid) { this.couponUid = couponUid; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }
    public String getCouponType() { return couponType; }
    public void setCouponType(String couponType) { this.couponType = couponType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(int maxRedemptions) { this.maxRedemptions = maxRedemptions; }
    public long getRedemptionsInPeriod() { return redemptionsInPeriod; }
    public void setRedemptionsInPeriod(long redemptionsInPeriod) { this.redemptionsInPeriod = redemptionsInPeriod; }
    public long getRedemptionsAllTime() { return redemptionsAllTime; }
    public void setRedemptionsAllTime(long redemptionsAllTime) { this.redemptionsAllTime = redemptionsAllTime; }
    public BigDecimal getDiscountInPeriod() { return discountInPeriod; }
    public void setDiscountInPeriod(BigDecimal discountInPeriod) { this.discountInPeriod = discountInPeriod; }
    public BigDecimal getUtilizationPct() { return utilizationPct; }
    public void setUtilizationPct(BigDecimal utilizationPct) { this.utilizationPct = utilizationPct; }
    public BigDecimal getAvgDiscountPerRedemption() { return avgDiscountPerRedemption; }
    public void setAvgDiscountPerRedemption(BigDecimal avgDiscountPerRedemption) { this.avgDiscountPerRedemption = avgDiscountPerRedemption; }
}
