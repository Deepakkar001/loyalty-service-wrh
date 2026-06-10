package com.loyaltyos.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CouponRedemptionListItem {

    private String redemptionUid;
    private String customerId;
    private String orderId;
    private String channel;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal pointsCredited;
    private String status;
    private Instant redeemedAt;

    public String getRedemptionUid() { return redemptionUid; }
    public void setRedemptionUid(String redemptionUid) { this.redemptionUid = redemptionUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPointsCredited() { return pointsCredited; }
    public void setPointsCredited(BigDecimal pointsCredited) { this.pointsCredited = pointsCredited; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
}
