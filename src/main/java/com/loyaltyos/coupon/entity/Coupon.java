package com.loyaltyos.coupon.entity;

import com.loyaltyos.coupon.enums.CouponStatus;
import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_uid", nullable = false, length = 128, unique = true)
    private String couponUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "coupon_code", nullable = false, length = 64)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 32)
    private CouponType couponType;

    @Column(name = "discount_value", precision = 18, scale = 4)
    private BigDecimal discountValue;

    @Column(name = "discount_pct", precision = 8, scale = 4)
    private BigDecimal discountPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CouponStatus status = CouponStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 16)
    private CouponUsageType usageType = CouponUsageType.SINGLE_USE;

    @Column(name = "max_redemptions", nullable = false)
    private int maxRedemptions = 1;

    @Column(name = "redemption_count", nullable = false)
    private int redemptionCount = 0;

    @Column(name = "max_redemptions_per_customer", nullable = false)
    private int maxRedemptionsPerCustomer = 1;

    @Column(name = "is_stackable", nullable = false)
    private boolean stackable = false;

    @Column(name = "target_customer_id", length = 128)
    private String targetCustomerId;

    @Column(name = "campaign_uid", length = 128)
    private String campaignUid;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "constraints_json", columnDefinition = "json")
    private String constraintsJson;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public String getCouponUid() { return couponUid; }
    public void setCouponUid(String couponUid) { this.couponUid = couponUid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
    public String getConstraintsJson() { return constraintsJson; }
    public void setConstraintsJson(String constraintsJson) { this.constraintsJson = constraintsJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
