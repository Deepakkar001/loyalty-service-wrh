package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class MerchantAgreementResponse {

    private String agreementUid;
    private String merchantUid;
    private String termsVersion;
    private LocalDate effectiveDate;
    private BigDecimal revenueSharePct;
    private String settlementCycle;
    private String pointsCurrency;
    private Integer expectedDailyTxnVolume;
    private String billingContactName;
    private String billingAddress;
    private String paymentMethod;
    private Integer contractDurationMonths;
    private Boolean autoRenewal;
    private BigDecimal proposedEarnRateMultiplier;
    private boolean merchantFundedCampaignsAllowed;
    private String signedByName;
    private String signedByEmail;
    private String signedByDesignation;
    private Instant signedAt;
    private String submittedByEmail;
    private String status;

    public String getAgreementUid() { return agreementUid; }
    public void setAgreementUid(String agreementUid) { this.agreementUid = agreementUid; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

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

    public boolean isMerchantFundedCampaignsAllowed() { return merchantFundedCampaignsAllowed; }
    public void setMerchantFundedCampaignsAllowed(boolean merchantFundedCampaignsAllowed) {
        this.merchantFundedCampaignsAllowed = merchantFundedCampaignsAllowed;
    }

    public String getSignedByName() { return signedByName; }
    public void setSignedByName(String signedByName) { this.signedByName = signedByName; }

    public String getSignedByEmail() { return signedByEmail; }
    public void setSignedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; }

    public String getSignedByDesignation() { return signedByDesignation; }
    public void setSignedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }

    public String getSubmittedByEmail() { return submittedByEmail; }
    public void setSubmittedByEmail(String submittedByEmail) { this.submittedByEmail = submittedByEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
