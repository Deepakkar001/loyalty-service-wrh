package com.loyaltyos.merchants.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateSettlementDisputeRequest {

    @NotBlank
    private String lineItemUid;

    @NotBlank
    private String reason;

    public String getLineItemUid() { return lineItemUid; }
    public void setLineItemUid(String lineItemUid) { this.lineItemUid = lineItemUid; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
