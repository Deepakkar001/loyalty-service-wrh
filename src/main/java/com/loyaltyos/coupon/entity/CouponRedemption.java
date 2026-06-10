package com.loyaltyos.coupon.entity;

import com.loyaltyos.coupon.enums.CouponRedemptionStatus;
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
@Table(name = "coupon_redemptions")
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "redemption_uid", nullable = false, length = 128, unique = true)
    private String redemptionUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "coupon_uid", nullable = false, length = 128)
    private String couponUid;

    @Column(name = "coupon_code", nullable = false, length = 64)
    private String couponCode;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "order_id", nullable = false, length = 128)
    private String orderId;

    @Column(name = "channel", length = 64)
    private String channel;

    @Column(name = "order_amount", precision = 18, scale = 4)
    private BigDecimal orderAmount;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "points_credited", precision = 18, scale = 4)
    private BigDecimal pointsCredited;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CouponRedemptionStatus status = CouponRedemptionStatus.REDEEMED;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;

    public Long getId() { return id; }
    public String getRedemptionUid() { return redemptionUid; }
    public void setRedemptionUid(String redemptionUid) { this.redemptionUid = redemptionUid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCouponUid() { return couponUid; }
    public void setCouponUid(String couponUid) { this.couponUid = couponUid; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
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
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public CouponRedemptionStatus getStatus() { return status; }
    public void setStatus(CouponRedemptionStatus status) { this.status = status; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
}
