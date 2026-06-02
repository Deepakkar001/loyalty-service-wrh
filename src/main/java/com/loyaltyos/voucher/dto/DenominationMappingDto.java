package com.loyaltyos.voucher.dto;

import java.math.BigDecimal;

public class DenominationMappingDto {

    private String mappingUid;
    private BigDecimal pointsRequired;
    private BigDecimal faceValue;
    private String currency;
    private String description;
    private String partnerSku;
    private int priority;
    private Long available;

    public String getMappingUid() { return mappingUid; }
    public void setMappingUid(String mappingUid) { this.mappingUid = mappingUid; }
    public BigDecimal getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(BigDecimal pointsRequired) { this.pointsRequired = pointsRequired; }
    public BigDecimal getFaceValue() { return faceValue; }
    public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPartnerSku() { return partnerSku; }
    public void setPartnerSku(String partnerSku) { this.partnerSku = partnerSku; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public Long getAvailable() { return available; }
    public void setAvailable(Long available) { this.available = available; }
}
