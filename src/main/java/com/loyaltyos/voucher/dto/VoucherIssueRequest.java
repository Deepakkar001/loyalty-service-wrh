package com.loyaltyos.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class VoucherIssueRequest {

    @NotBlank
    @Size(max = 64)
    private String programmeUid;

    @NotBlank
    @Size(max = 64)
    private String catalogRewardUid;

    @NotBlank
    @Size(max = 128)
    private String customerId;

    @NotBlank
    @Size(max = 128)
    private String redemptionId;

    /** Required for multi-denomination catalogs: must match a configured tier exactly. */
    private BigDecimal pointsToRedeem;

    /** Alternative to pointsToRedeem: pick tier by voucher face value. */
    private BigDecimal faceValue;

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public BigDecimal getPointsToRedeem() { return pointsToRedeem; }
    public void setPointsToRedeem(BigDecimal pointsToRedeem) { this.pointsToRedeem = pointsToRedeem; }
    public BigDecimal getFaceValue() { return faceValue; }
    public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
}
