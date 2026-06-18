package com.loyaltyos.merchants.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

public class MerchantPendingFinanceAgreementResponse {

    private String agreementUid;
    private String merchantUid;
    private String merchantLegalName;
    private String termsVersion;
    private LocalDate effectiveDate;
    private BigDecimal revenueSharePct;
    private String settlementCycle;
    private BigDecimal proposedEarnRateMultiplier;
    private String submittedByEmail;
    private Instant signedAt;

    public String getAgreementUid() { return agreementUid; }
    public void setAgreementUid(String agreementUid) { this.agreementUid = agreementUid; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getMerchantLegalName() { return merchantLegalName; }
    public void setMerchantLegalName(String merchantLegalName) { this.merchantLegalName = merchantLegalName; }

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public BigDecimal getRevenueSharePct() { return revenueSharePct; }
    public void setRevenueSharePct(BigDecimal revenueSharePct) { this.revenueSharePct = revenueSharePct; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }

    public BigDecimal getProposedEarnRateMultiplier() { return proposedEarnRateMultiplier; }
    public void setProposedEarnRateMultiplier(BigDecimal proposedEarnRateMultiplier) {
        this.proposedEarnRateMultiplier = proposedEarnRateMultiplier;
    }

    public String getSubmittedByEmail() { return submittedByEmail; }
    public void setSubmittedByEmail(String submittedByEmail) { this.submittedByEmail = submittedByEmail; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
}
