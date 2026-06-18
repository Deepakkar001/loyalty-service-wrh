package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;

/**
 * Merchant profile fields shown read-only on the agreement step.
 */
public class MerchantAgreementPrefillResponse {

    private String merchantUid;
    private String legalName;
    private String displayName;
    private String category;
    private String contactEmail;
    private String contactPhone;
    private String taxId;
    private BigDecimal earnRateMultiplier;
    private String settlementCycle;
    private boolean hasExistingAgreement;

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

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

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public BigDecimal getEarnRateMultiplier() { return earnRateMultiplier; }
    public void setEarnRateMultiplier(BigDecimal earnRateMultiplier) {
        this.earnRateMultiplier = earnRateMultiplier;
    }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }

    public boolean isHasExistingAgreement() { return hasExistingAgreement; }
    public void setHasExistingAgreement(boolean hasExistingAgreement) {
        this.hasExistingAgreement = hasExistingAgreement;
    }
}
