package com.loyaltyos.coupon.dto;

import com.loyaltyos.coupon.enums.CouponType;
import java.math.BigDecimal;
import java.time.Instant;

public class CouponRedeemResponse {

    private String status;
    private String redemptionUid;
    private String couponUid;
    private String couponCode;
    private CouponType couponType;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal pointsCredited;
    private BigDecimal newBalance;
    private Long ledgerId;
    private boolean idempotentReplay;
    private Instant redeemedAt;
    private String message;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRedemptionUid() { return redemptionUid; }
    public void setRedemptionUid(String redemptionUid) { this.redemptionUid = redemptionUid; }
    public String getCouponUid() { return couponUid; }
    public void setCouponUid(String couponUid) { this.couponUid = couponUid; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public CouponType getCouponType() { return couponType; }
    public void setCouponType(CouponType couponType) { this.couponType = couponType; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public BigDecimal getPointsCredited() { return pointsCredited; }
    public void setPointsCredited(BigDecimal pointsCredited) { this.pointsCredited = pointsCredited; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
    public void setIdempotentReplay(boolean idempotentReplay) { this.idempotentReplay = idempotentReplay; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
