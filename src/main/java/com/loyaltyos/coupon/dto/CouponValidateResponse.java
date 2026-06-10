package com.loyaltyos.coupon.dto;

import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponValidationReason;
import java.math.BigDecimal;
import java.time.Instant;

public class CouponValidateResponse {

    private boolean valid;
    private String status;
    private CouponValidationReason reason;
    private String message;
    private String couponUid;
    private String couponCode;
    private String couponName;
    private CouponType couponType;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal pointsToCredit;
    private String freeItemSku;
    private String freeItemLabel;
    private boolean stackable;
    private Instant validUntil;
    private String currency;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public CouponValidationReason getReason() { return reason; }
    public void setReason(CouponValidationReason reason) { this.reason = reason; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCouponUid() { return couponUid; }
    public void setCouponUid(String couponUid) { this.couponUid = couponUid; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }
    public CouponType getCouponType() { return couponType; }
    public void setCouponType(CouponType couponType) { this.couponType = couponType; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public BigDecimal getPointsToCredit() { return pointsToCredit; }
    public void setPointsToCredit(BigDecimal pointsToCredit) { this.pointsToCredit = pointsToCredit; }
    public String getFreeItemSku() { return freeItemSku; }
    public void setFreeItemSku(String freeItemSku) { this.freeItemSku = freeItemSku; }
    public String getFreeItemLabel() { return freeItemLabel; }
    public void setFreeItemLabel(String freeItemLabel) { this.freeItemLabel = freeItemLabel; }
    public boolean isStackable() { return stackable; }
    public void setStackable(boolean stackable) { this.stackable = stackable; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
