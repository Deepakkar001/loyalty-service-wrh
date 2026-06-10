package com.loyaltyos.coupon.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Flexible constraints stored in coupons.constraints_json so tenants are not locked to fixed fields.
 */
public class CouponConstraints {

    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountCap;
    private List<String> allowedChannels = new ArrayList<>();
    private List<String> categoryIds = new ArrayList<>();
    private String freeItemSku;
    private String freeItemLabel;
    private String currency = "INR";

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public BigDecimal getMaxDiscountCap() { return maxDiscountCap; }
    public void setMaxDiscountCap(BigDecimal maxDiscountCap) { this.maxDiscountCap = maxDiscountCap; }
    public List<String> getAllowedChannels() { return allowedChannels; }
    public void setAllowedChannels(List<String> allowedChannels) {
        this.allowedChannels = allowedChannels != null ? allowedChannels : new ArrayList<>();
    }
    public List<String> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds != null ? categoryIds : new ArrayList<>();
    }
    public String getFreeItemSku() { return freeItemSku; }
    public void setFreeItemSku(String freeItemSku) { this.freeItemSku = freeItemSku; }
    public String getFreeItemLabel() { return freeItemLabel; }
    public void setFreeItemLabel(String freeItemLabel) { this.freeItemLabel = freeItemLabel; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
