package com.loyaltyos.merchants.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Structured merchant partner agreement (mirrors tenant {@code SubmitAgreementRequest}).
 * Replaces URL-only {@link UpdateMerchantAgreementRequest} for new submissions.
 */
public class SubmitMerchantAgreementRequest {

    @NotBlank
    @Size(max = 20)
    private String termsVersion;

    @NotNull
    private LocalDate effectiveDate;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal revenueSharePct;

    @NotBlank
    @Size(max = 32)
    private String settlementCycle;

    @NotBlank
    @Size(max = 10)
    private String pointsCurrency;

    private Integer expectedDailyTxnVolume;

    @Size(max = 255)
    private String billingContactName;

    @Size(max = 2000)
    private String billingAddress;

    @Size(max = 30)
    private String paymentMethod;

    @NotNull
    private Integer contractDurationMonths;

    private Boolean autoRenewal;

    @DecimalMin("0.1")
    @DecimalMax("10.0")
    private BigDecimal proposedEarnRateMultiplier;

    private Boolean merchantFundedCampaignsAllowed;

    @NotBlank
    @Size(min = 2, max = 255)
    private String signedByName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String signedByEmail;

    @Size(max = 255)
    private String signedByDesignation;

    @NotNull
    private Boolean termsAccepted;

    /** Optional portal login email; when set, updates merchant contact before recording agreement. */
    @Email
    @Size(max = 255)
    private String portalContactEmail;

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public BigDecimal getRevenueSharePct() { return revenueSharePct; }
    public void setRevenueSharePct(BigDecimal revenueSharePct) { this.revenueSharePct = revenueSharePct; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }

    public String getPointsCurrency() { return pointsCurrency; }
    public void setPointsCurrency(String pointsCurrency) { this.pointsCurrency = pointsCurrency; }

    public Integer getExpectedDailyTxnVolume() { return expectedDailyTxnVolume; }
    public void setExpectedDailyTxnVolume(Integer expectedDailyTxnVolume) {
        this.expectedDailyTxnVolume = expectedDailyTxnVolume;
    }

    public String getBillingContactName() { return billingContactName; }
    public void setBillingContactName(String billingContactName) { this.billingContactName = billingContactName; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getContractDurationMonths() { return contractDurationMonths; }
    public void setContractDurationMonths(Integer contractDurationMonths) {
        this.contractDurationMonths = contractDurationMonths;
    }

    public Boolean getAutoRenewal() { return autoRenewal; }
    public void setAutoRenewal(Boolean autoRenewal) { this.autoRenewal = autoRenewal; }

    public BigDecimal getProposedEarnRateMultiplier() { return proposedEarnRateMultiplier; }
    public void setProposedEarnRateMultiplier(BigDecimal proposedEarnRateMultiplier) {
        this.proposedEarnRateMultiplier = proposedEarnRateMultiplier;
    }

    public Boolean getMerchantFundedCampaignsAllowed() { return merchantFundedCampaignsAllowed; }
    public void setMerchantFundedCampaignsAllowed(Boolean merchantFundedCampaignsAllowed) {
        this.merchantFundedCampaignsAllowed = merchantFundedCampaignsAllowed;
    }

    public String getSignedByName() { return signedByName; }
    public void setSignedByName(String signedByName) { this.signedByName = signedByName; }

    public String getSignedByEmail() { return signedByEmail; }
    public void setSignedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; }

    public String getSignedByDesignation() { return signedByDesignation; }
    public void setSignedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; }

    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public String getPortalContactEmail() { return portalContactEmail; }
    public void setPortalContactEmail(String portalContactEmail) { this.portalContactEmail = portalContactEmail; }
}
