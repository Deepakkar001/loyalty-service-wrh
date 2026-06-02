package com.loyaltyos.voucher.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class VoucherCsvRow {

    private String code;
    private String pin;
    private BigDecimal faceValue;
    private String currency;
    private Instant expiresAt;
    private String partnerSku;
    private String serial;
    private String region;
    private String channel;
    private String externalRef;

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
    public String getPartnerSku() { return partnerSku; }
    public void setPartnerSku(String partnerSku) { this.partnerSku = partnerSku; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
}
