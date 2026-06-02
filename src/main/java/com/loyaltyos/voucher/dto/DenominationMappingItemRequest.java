package com.loyaltyos.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class DenominationMappingItemRequest {

    @NotNull
    private BigDecimal pointsRequired;

    @NotNull
    private BigDecimal faceValue;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 512)
    private String description;

    @Size(max = 128)
    private String partnerSku;

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
}
