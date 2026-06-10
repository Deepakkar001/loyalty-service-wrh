package com.loyaltyos.coupon.dto;

import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import com.loyaltyos.coupon.model.CouponConstraints;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public class CouponCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String programmeUid = "default";

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 64)
    private String couponCode;

    @NotNull
    private CouponType couponType;

    private BigDecimal discountValue;
    private BigDecimal discountPct;

    @NotNull
    private CouponUsageType usageType = CouponUsageType.SINGLE_USE;

    private int maxRedemptions = 1;
    private int maxRedemptionsPerCustomer = 1;
    private boolean stackable = false;

    @Size(max = 128)
    private String targetCustomerId;

    @Size(max = 128)
    private String campaignUid;

    private Instant validFrom;

    @NotNull
    private Instant validUntil;

    @Valid
    private CouponConstraints constraints;

    private boolean activateImmediately = false;

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
    public CouponUsageType getUsageType() { return usageType; }
    public void setUsageType(CouponUsageType usageType) { this.usageType = usageType; }
    public int getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(int maxRedemptions) { this.maxRedemptions = maxRedemptions; }
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
    public boolean isActivateImmediately() { return activateImmediately; }
    public void setActivateImmediately(boolean activateImmediately) { this.activateImmediately = activateImmediately; }
}
