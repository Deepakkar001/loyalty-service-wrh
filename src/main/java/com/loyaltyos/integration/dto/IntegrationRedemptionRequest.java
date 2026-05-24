package com.loyaltyos.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class IntegrationRedemptionRequest {

    @NotBlank
    @Size(max = 128)
    private String redemptionId;

    @NotBlank
    @Size(max = 128)
    private String customerId;

    private String programmeUid = "default";

    @NotNull
    @Positive
    private BigDecimal pointsToRedeem;

    private BigDecimal orderAmount;
    private String currency;
    private String channel;

    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public BigDecimal getPointsToRedeem() { return pointsToRedeem; }
    public void setPointsToRedeem(BigDecimal pointsToRedeem) { this.pointsToRedeem = pointsToRedeem; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
