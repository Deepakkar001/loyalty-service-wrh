package com.loyaltyos.coupon.dto;

import com.loyaltyos.coupon.enums.CouponStatus;
import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import com.loyaltyos.coupon.model.CouponConstraints;
import java.math.BigDecimal;
import java.time.Instant;

public class CouponResponse {

    private String couponUid;
    private String programmeUid;
    private String name;
    private String description;
    private String couponCode;
    private CouponType couponType;
    private BigDecimal discountValue;
    private BigDecimal discountPct;
    private CouponStatus status;
    private CouponUsageType usageType;
    private int maxRedemptions;
    private int redemptionCount;
    private int maxRedemptionsPerCustomer;
    private boolean stackable;
    private String targetCustomerId;
    private String campaignUid;
    private Instant validFrom;
    private Instant validUntil;
    private CouponConstraints constraints;
    private Instant createdAt;
    private Instant updatedAt;

    public String getCouponUid() { return couponUid; }
    public void setCouponUid(String couponUid) { this.couponUid = couponUid; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public CouponType getCouponType() { return couponType; }
    public void setCouponType(CouponType couponType) { this.couponType = couponType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
    public CouponStatus getStatus() { return status; }
    public void setStatus(CouponStatus status) { this.status = status; }
    public CouponUsageType getUsageType() { return usageType; }
    public void setUsageType(CouponUsageType usageType) { this.usageType = usageType; }
    public int getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(int maxRedemptions) { this.maxRedemptions = maxRedemptions; }
    public int getRedemptionCount() { return redemptionCount; }
    public void setRedemptionCount(int redemptionCount) { this.redemptionCount = redemptionCount; }
    public int getMaxRedemptionsPerCustomer() { return maxRedemptionsPerCustomer; }
    public void setMaxRedemptionsPerCustomer(int maxRedemptionsPerCustomer) {
        this.maxRedemptionsPerCustomer = maxRedemptionsPerCustomer;
    }
    public boolean isStackable() { return stackable; }
    public void setStackable(boolean stackable) { this.stackable = stackable; }
    public String getTargetCustomerId() { return targetCustomerId; }
    public void setTargetCustomerId(String targetCustomerId) { this.targetCustomerId = targetCustomerId; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public CouponConstraints getConstraints() { return constraints; }
    public void setConstraints(CouponConstraints constraints) { this.constraints = constraints; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
