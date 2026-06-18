package com.loyaltyos.merchants.dto;

import java.time.Instant;

public class MerchantPendingConfigApprovalResponse {

    private String requestUid;
    private String merchantUid;
    private String merchantLegalName;
    private String requestedBy;
    private Instant requestedAt;
    private String payloadJson;

    public String getRequestUid() { return requestUid; }
    public void setRequestUid(String requestUid) { this.requestUid = requestUid; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getMerchantLegalName() { return merchantLegalName; }
    public void setMerchantLegalName(String merchantLegalName) { this.merchantLegalName = merchantLegalName; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
