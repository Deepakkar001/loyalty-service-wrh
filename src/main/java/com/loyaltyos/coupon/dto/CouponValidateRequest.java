package com.loyaltyos.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CouponValidateRequest {

    @NotBlank
    @Size(max = 64)
    private String programmeUid = "default";

    @NotBlank
    @Size(max = 128)
    private String customerId;

    private BigDecimal orderAmount;

    @Size(max = 64)
    private String channel;

    private List<String> otherCouponsApplied = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public List<String> getOtherCouponsApplied() { return otherCouponsApplied; }
    public void setOtherCouponsApplied(List<String> otherCouponsApplied) {
        this.otherCouponsApplied = otherCouponsApplied != null ? otherCouponsApplied : new ArrayList<>();
    }
}
