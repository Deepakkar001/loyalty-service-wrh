package com.loyaltyos.merchants.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class CreateMerchantRequest {

    @NotBlank
    private String legalName;

    private String displayName;

    @NotBlank
    private String category;

    @Email
    @NotBlank
    private String contactEmail;

    private String contactPhone;

    private String bankDetailsVaultRef;

    @NotBlank
    private String taxId;

    private BigDecimal earnRateMultiplier;

    private String settlementCycle;

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getBankDetailsVaultRef() { return bankDetailsVaultRef; }
    public void setBankDetailsVaultRef(String bankDetailsVaultRef) { this.bankDetailsVaultRef = bankDetailsVaultRef; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public BigDecimal getEarnRateMultiplier() { return earnRateMultiplier; }
    public void setEarnRateMultiplier(BigDecimal earnRateMultiplier) { this.earnRateMultiplier = earnRateMultiplier; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }
}
