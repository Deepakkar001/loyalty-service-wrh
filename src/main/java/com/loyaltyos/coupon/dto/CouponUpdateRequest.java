package com.loyaltyos.coupon.dto;

import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import com.loyaltyos.coupon.model.CouponConstraints;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public class CouponUpdateRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private CouponType couponType;
    private BigDecimal discountValue;
    private BigDecimal discountPct;
    private CouponUsageType usageType;
    private Integer maxRedemptions;
    private Integer maxRedemptionsPerCustomer;
    private Boolean stackable;

    @Size(max = 128)
    private String targetCustomerId;

    @Size(max = 128)
    private String campaignUid;

    private Instant validFrom;
    private Instant validUntil;

    @Valid
    private CouponConstraints constraints;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CouponType getCouponType() { return couponType; }
    public void setCouponType(CouponType couponType) { this.couponType = couponType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
    public CouponUsageType getUsageType() { return usageType; }
    public void setUsageType(CouponUsageType usageType) { this.usageType = usageType; }
    public Integer getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(Integer maxRedemptions) { this.maxRedemptions = maxRedemptions; }
    public Integer getMaxRedemptionsPerCustomer() { return maxRedemptionsPerCustomer; }
    public void setMaxRedemptionsPerCustomer(Integer maxRedemptionsPerCustomer) {
        this.maxRedemptionsPerCustomer = maxRedemptionsPerCustomer;
    }
    public Boolean getStackable() { return stackable; }
    public void setStackable(Boolean stackable) { this.stackable = stackable; }
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
}
