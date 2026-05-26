package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class IntegrationRedemptionResponse {

    private String status;
    private String redemptionId;
    private String customerId;
    private String programmeUid;
    private BigDecimal pointsRedeemed;
    private BigDecimal newBalance;
    private Long ledgerId;
    private boolean idempotentReplay;
    private Instant timestamp;
    private String catalogRewardUid;
    private String catalogRewardName;
    private String catalogRewardType;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public BigDecimal getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(BigDecimal pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
    public void setIdempotentReplay(boolean idempotentReplay) { this.idempotentReplay = idempotentReplay; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public String getCatalogRewardName() { return catalogRewardName; }
    public void setCatalogRewardName(String catalogRewardName) { this.catalogRewardName = catalogRewardName; }
    public String getCatalogRewardType() { return catalogRewardType; }
    public void setCatalogRewardType(String catalogRewardType) { this.catalogRewardType = catalogRewardType; }
}
