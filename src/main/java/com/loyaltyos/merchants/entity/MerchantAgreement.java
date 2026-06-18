package com.loyaltyos.merchants.entity;

import com.loyaltyos.merchants.enums.MerchantAgreementStatus;
import com.loyaltyos.merchants.enums.SettlementCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "merchant_agreements",
    indexes = @Index(name = "idx_merchant_agreements_merchant", columnList = "tenant_id, merchant_uid")
)
public class MerchantAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "merchant_uid", nullable = false, length = 128)
    private String merchantUid;

    @Column(name = "agreement_uid", nullable = false, unique = true, length = 128)
    private String agreementUid;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "revenue_share_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal revenueSharePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_cycle", nullable = false, length = 32)
    private SettlementCycle settlementCycle;

    @Column(name = "points_currency", nullable = false, length = 10)
    private String pointsCurrency = "INR";

    @Column(name = "expected_daily_txn_volume")
    private Integer expectedDailyTxnVolume;

    @Column(name = "billing_contact_name", length = 255)
    private String billingContactName;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "contract_duration_months", nullable = false)
    private Integer contractDurationMonths = 12;

    @Column(name = "auto_renewal", nullable = false)
    private Boolean autoRenewal = true;

    @Column(name = "proposed_earn_rate_multiplier", precision = 6, scale = 3)
    private BigDecimal proposedEarnRateMultiplier;

    @Column(name = "merchant_funded_campaigns_allowed", nullable = false)
    private boolean merchantFundedCampaignsAllowed = true;

    @Column(name = "signed_by_name", nullable = false, length = 255)
    private String signedByName;

    @Column(name = "signed_by_email", nullable = false, length = 255)
    private String signedByEmail;

    @Column(name = "signed_by_designation", length = 255)
    private String signedByDesignation;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "submitted_by_email", nullable = false, length = 255)
    private String submittedByEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MerchantAgreementStatus status = MerchantAgreementStatus.APPROVED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MerchantAgreement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getAgreementUid() { return agreementUid; }
    public void setAgreementUid(String agreementUid) { this.agreementUid = agreementUid; }

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public BigDecimal getRevenueSharePct() { return revenueSharePct; }
    public void setRevenueSharePct(BigDecimal revenueSharePct) { this.revenueSharePct = revenueSharePct; }

    public SettlementCycle getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(SettlementCycle settlementCycle) { this.settlementCycle = settlementCycle; }

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

    public MerchantAgreementStatus getStatus() { return status; }
    public void setStatus(MerchantAgreementStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
