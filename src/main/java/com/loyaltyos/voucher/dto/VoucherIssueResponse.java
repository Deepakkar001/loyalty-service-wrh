package com.loyaltyos.voucher.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class VoucherIssueResponse {

    private String status;
    private String redemptionId;
    private BigDecimal pointsRedeemed;
    private BigDecimal newBalance;
    private Long ledgerId;
    private String catalogRewardUid;
    private boolean idempotentReplay;
    private VoucherDetails voucher;
    private SelectedDenomination selectedDenomination;
    private String errorMessage;
    private boolean retryable;
    private Instant timestamp;

    public static class SelectedDenomination {
        private BigDecimal pointsRequired;
        private BigDecimal faceValue;
        private String currency;

        public BigDecimal getPointsRequired() { return pointsRequired; }
        public void setPointsRequired(BigDecimal pointsRequired) { this.pointsRequired = pointsRequired; }
        public BigDecimal getFaceValue() { return faceValue; }
        public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class VoucherDetails {
        private String code;
        private String pin;
        private BigDecimal faceValue;
        private String currency;
        private Instant expiresAt;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
        public BigDecimal getFaceValue() { return faceValue; }
        public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public BigDecimal getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(BigDecimal pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
    public void setIdempotentReplay(boolean idempotentReplay) { this.idempotentReplay = idempotentReplay; }
    public VoucherDetails getVoucher() { return voucher; }
    public void setVoucher(VoucherDetails voucher) { this.voucher = voucher; }
    public SelectedDenomination getSelectedDenomination() { return selectedDenomination; }
    public void setSelectedDenomination(SelectedDenomination selectedDenomination) {
        this.selectedDenomination = selectedDenomination;
    }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
