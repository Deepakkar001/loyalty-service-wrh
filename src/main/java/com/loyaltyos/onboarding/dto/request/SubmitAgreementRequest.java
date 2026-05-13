package com.loyaltyos.onboarding.dto.request;

import com.loyaltyos.onboarding.domain.enums.SettlementFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SubmitAgreementRequest {

    @NotBlank(message = "Terms version is required")
    @Size(max = 20, message = "Terms version must be at most 20 characters")
    private String termsVersion;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    @NotNull(message = "Revenue share % is required")
    @DecimalMin(value = "0.0", message = "Revenue share % must be at least 0")
    @DecimalMax(value = "100.0", message = "Revenue share % must be at most 100")
    private BigDecimal revenueSharePct;

    @NotNull(message = "Settlement frequency is required")
    private SettlementFrequency settlementFrequency;

    @NotBlank(message = "Points currency is required")
    @Size(max = 10)
    private String pointsCurrency;

    private Integer expectedDailyTxnVolume;

    @Size(max = 255)
    private String billingContactName;

    @Size(max = 2000)
    private String billingAddress;

    @Size(max = 30)
    private String paymentMethod;

    private Integer contractDurationMonths;

    private Boolean autoRenewal;

    @NotBlank(message = "Signed by name is required")
    @Size(min = 2, max = 255, message = "Signed by name must be between 2 and 255 characters")
    private String signedByName;

    @NotBlank(message = "Signed by email is required")
    @Email(message = "Signed by email must be a valid email address")
    private String signedByEmail;

    @Size(max = 255, message = "Designation must be at most 255 characters")
    private String signedByDesignation;

    public SubmitAgreementRequest() {}

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public BigDecimal getRevenueSharePct() { return revenueSharePct; }
    public void setRevenueSharePct(BigDecimal revenueSharePct) { this.revenueSharePct = revenueSharePct; }
    public SettlementFrequency getSettlementFrequency() { return settlementFrequency; }
    public void setSettlementFrequency(SettlementFrequency settlementFrequency) { this.settlementFrequency = settlementFrequency; }
    public String getPointsCurrency() { return pointsCurrency; }
    public void setPointsCurrency(String pointsCurrency) { this.pointsCurrency = pointsCurrency; }
    public Integer getExpectedDailyTxnVolume() { return expectedDailyTxnVolume; }
    public void setExpectedDailyTxnVolume(Integer expectedDailyTxnVolume) { this.expectedDailyTxnVolume = expectedDailyTxnVolume; }
    public String getBillingContactName() { return billingContactName; }
    public void setBillingContactName(String billingContactName) { this.billingContactName = billingContactName; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Integer getContractDurationMonths() { return contractDurationMonths; }
    public void setContractDurationMonths(Integer contractDurationMonths) { this.contractDurationMonths = contractDurationMonths; }
    public Boolean getAutoRenewal() { return autoRenewal; }
    public void setAutoRenewal(Boolean autoRenewal) { this.autoRenewal = autoRenewal; }
    public String getSignedByName() { return signedByName; }
    public void setSignedByName(String signedByName) { this.signedByName = signedByName; }
    public String getSignedByEmail() { return signedByEmail; }
    public void setSignedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; }
    public String getSignedByDesignation() { return signedByDesignation; }
    public void setSignedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; }
}

