package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.SettlementFrequency;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tenant_agreements")
public class TenantAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "agreement_uid", nullable = false, unique = true, length = 128)
    private String agreementUid;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "revenue_share_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal revenueSharePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_frequency", nullable = false, length = 50)
    private SettlementFrequency settlementFrequency;

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

    @Column(name = "document_s3_key", length = 500)
    private String documentS3Key;

    @Column(name = "signed_by_name", nullable = false, length = 255)
    private String signedByName;

    @Column(name = "signed_by_email", nullable = false, length = 255)
    private String signedByEmail;

    @Column(name = "signed_by_designation", length = 255)
    private String signedByDesignation;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AgreementStatus status = AgreementStatus.PENDING_APPROVAL;

    @Column(name = "approved_by_admin_id", length = 128)
    private String approvedByAdminId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public TenantAgreement() {}

    public TenantAgreement(
        Long id,
        String tenantId,
        String agreementUid,
        String termsVersion,
        LocalDate effectiveDate,
        BigDecimal revenueSharePct,
        SettlementFrequency settlementFrequency,
        String pointsCurrency,
        Integer expectedDailyTxnVolume,
        String billingContactName,
        String billingAddress,
        String paymentMethod,
        Integer contractDurationMonths,
        Boolean autoRenewal,
        String documentS3Key,
        String signedByName,
        String signedByEmail,
        String signedByDesignation,
        Instant signedAt,
        AgreementStatus status,
        String approvedByAdminId,
        Instant approvedAt,
        String rejectionReason,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.agreementUid = agreementUid;
        this.termsVersion = termsVersion;
        this.effectiveDate = effectiveDate;
        this.revenueSharePct = revenueSharePct;
        this.settlementFrequency = settlementFrequency;
        this.pointsCurrency = pointsCurrency != null ? pointsCurrency : "INR";
        this.expectedDailyTxnVolume = expectedDailyTxnVolume;
        this.billingContactName = billingContactName;
        this.billingAddress = billingAddress;
        this.paymentMethod = paymentMethod;
        this.contractDurationMonths = contractDurationMonths != null ? contractDurationMonths : 12;
        this.autoRenewal = autoRenewal != null ? autoRenewal : true;
        this.documentS3Key = documentS3Key;
        this.signedByName = signedByName;
        this.signedByEmail = signedByEmail;
        this.signedByDesignation = signedByDesignation;
        this.signedAt = signedAt;
        this.status = status != null ? status : AgreementStatus.PENDING_APPROVAL;
        this.approvedByAdminId = approvedByAdminId;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String agreementUid;
        private String termsVersion;
        private LocalDate effectiveDate;
        private BigDecimal revenueSharePct;
        private SettlementFrequency settlementFrequency;
        private String pointsCurrency = "INR";
        private Integer expectedDailyTxnVolume;
        private String billingContactName;
        private String billingAddress;
        private String paymentMethod;
        private Integer contractDurationMonths = 12;
        private Boolean autoRenewal = true;
        private String documentS3Key;
        private String signedByName;
        private String signedByEmail;
        private String signedByDesignation;
        private Instant signedAt;
        private AgreementStatus status = AgreementStatus.PENDING_APPROVAL;
        private String approvedByAdminId;
        private Instant approvedAt;
        private String rejectionReason;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder agreementUid(String agreementUid) { this.agreementUid = agreementUid; return this; }
        public Builder termsVersion(String termsVersion) { this.termsVersion = termsVersion; return this; }
        public Builder effectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; return this; }
        public Builder revenueSharePct(BigDecimal revenueSharePct) { this.revenueSharePct = revenueSharePct; return this; }
        public Builder settlementFrequency(SettlementFrequency settlementFrequency) { this.settlementFrequency = settlementFrequency; return this; }
        public Builder pointsCurrency(String pointsCurrency) { this.pointsCurrency = pointsCurrency; return this; }
        public Builder expectedDailyTxnVolume(Integer expectedDailyTxnVolume) { this.expectedDailyTxnVolume = expectedDailyTxnVolume; return this; }
        public Builder billingContactName(String billingContactName) { this.billingContactName = billingContactName; return this; }
        public Builder billingAddress(String billingAddress) { this.billingAddress = billingAddress; return this; }
        public Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder contractDurationMonths(Integer contractDurationMonths) { this.contractDurationMonths = contractDurationMonths; return this; }
        public Builder autoRenewal(Boolean autoRenewal) { this.autoRenewal = autoRenewal; return this; }
        public Builder documentS3Key(String documentS3Key) { this.documentS3Key = documentS3Key; return this; }
        public Builder signedByName(String signedByName) { this.signedByName = signedByName; return this; }
        public Builder signedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; return this; }
        public Builder signedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; return this; }
        public Builder signedAt(Instant signedAt) { this.signedAt = signedAt; return this; }
        public Builder status(AgreementStatus status) { this.status = status; return this; }
        public Builder approvedByAdminId(String approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; return this; }
        public Builder approvedAt(Instant approvedAt) { this.approvedAt = approvedAt; return this; }
        public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TenantAgreement build() {
            return new TenantAgreement(
                id,
                tenantId,
                agreementUid,
                termsVersion,
                effectiveDate,
                revenueSharePct,
                settlementFrequency,
                pointsCurrency,
                expectedDailyTxnVolume,
                billingContactName,
                billingAddress,
                paymentMethod,
                contractDurationMonths,
                autoRenewal,
                documentS3Key,
                signedByName,
                signedByEmail,
                signedByDesignation,
                signedAt,
                status,
                approvedByAdminId,
                approvedAt,
                rejectionReason,
                createdAt
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getAgreementUid() { return agreementUid; }
    public void setAgreementUid(String agreementUid) { this.agreementUid = agreementUid; }
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
    public String getDocumentS3Key() { return documentS3Key; }
    public void setDocumentS3Key(String documentS3Key) { this.documentS3Key = documentS3Key; }
    public String getSignedByName() { return signedByName; }
    public void setSignedByName(String signedByName) { this.signedByName = signedByName; }
    public String getSignedByEmail() { return signedByEmail; }
    public void setSignedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; }
    public String getSignedByDesignation() { return signedByDesignation; }
    public void setSignedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
    public AgreementStatus getStatus() { return status; }
    public void setStatus(AgreementStatus status) { this.status = status; }
    public String getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(String approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

