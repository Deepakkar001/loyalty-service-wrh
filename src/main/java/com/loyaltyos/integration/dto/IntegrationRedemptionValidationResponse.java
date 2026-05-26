package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class IntegrationRedemptionValidationResponse {

    private String status;
    private String redemptionId;
    private Instant timestamp;
    private boolean valid;
    private BigDecimal currentBalance;
    private BigDecimal pointsToRedeem;
    private Map<String, String> fieldErrors = new LinkedHashMap<>();
    private String note;
    private String catalogRewardUid;
    private String catalogRewardName;
    private String catalogRewardType;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public BigDecimal getPointsToRedeem() { return pointsToRedeem; }
    public void setPointsToRedeem(BigDecimal pointsToRedeem) { this.pointsToRedeem = pointsToRedeem; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public String getCatalogRewardName() { return catalogRewardName; }
    public void setCatalogRewardName(String catalogRewardName) { this.catalogRewardName = catalogRewardName; }
    public String getCatalogRewardType() { return catalogRewardType; }
    public void setCatalogRewardType(String catalogRewardType) { this.catalogRewardType = catalogRewardType; }
}
