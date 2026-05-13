package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.SettlementFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class PendingAgreementListItem {
    private String agreementUid;
    private String tenantId;
    private String companyName;
    private String tenantEmail;
    private String termsVersion;
    private LocalDate effectiveDate;
    private BigDecimal revenueSharePct;
    private SettlementFrequency settlementFrequency;
    private String signedByName;
    private String signedByEmail;
    private String signedByDesignation;
    private Instant signedAt;
    private AgreementStatus status;
    private Instant createdAt;

    public PendingAgreementListItem() {}

    public PendingAgreementListItem(
        String agreementUid,
        String tenantId,
        String companyName,
        String tenantEmail,
        String termsVersion,
        LocalDate effectiveDate,
        BigDecimal revenueSharePct,
        SettlementFrequency settlementFrequency,
        String signedByName,
        String signedByEmail,
        String signedByDesignation,
        Instant signedAt,
        AgreementStatus status,
        Instant createdAt
    ) {
        this.agreementUid = agreementUid;
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.tenantEmail = tenantEmail;
        this.termsVersion = termsVersion;
        this.effectiveDate = effectiveDate;
        this.revenueSharePct = revenueSharePct;
        this.settlementFrequency = settlementFrequency;
        this.signedByName = signedByName;
        this.signedByEmail = signedByEmail;
        this.signedByDesignation = signedByDesignation;
        this.signedAt = signedAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String agreementUid;
        private String tenantId;
        private String companyName;
        private String tenantEmail;
        private String termsVersion;
        private LocalDate effectiveDate;
        private BigDecimal revenueSharePct;
        private SettlementFrequency settlementFrequency;
        private String signedByName;
        private String signedByEmail;
        private String signedByDesignation;
        private Instant signedAt;
        private AgreementStatus status;
        private Instant createdAt;

        private Builder() {}

        public Builder agreementUid(String agreementUid) { this.agreementUid = agreementUid; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder tenantEmail(String tenantEmail) { this.tenantEmail = tenantEmail; return this; }
        public Builder termsVersion(String termsVersion) { this.termsVersion = termsVersion; return this; }
        public Builder effectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; return this; }
        public Builder revenueSharePct(BigDecimal revenueSharePct) { this.revenueSharePct = revenueSharePct; return this; }
        public Builder settlementFrequency(SettlementFrequency settlementFrequency) { this.settlementFrequency = settlementFrequency; return this; }
        public Builder signedByName(String signedByName) { this.signedByName = signedByName; return this; }
        public Builder signedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; return this; }
        public Builder signedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; return this; }
        public Builder signedAt(Instant signedAt) { this.signedAt = signedAt; return this; }
        public Builder status(AgreementStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public PendingAgreementListItem build() {
            return new PendingAgreementListItem(
                agreementUid,
                tenantId,
                companyName,
                tenantEmail,
                termsVersion,
                effectiveDate,
                revenueSharePct,
                settlementFrequency,
                signedByName,
                signedByEmail,
                signedByDesignation,
                signedAt,
                status,
                createdAt
            );
        }
    }

    public String getAgreementUid() { return agreementUid; }
    public String getTenantId() { return tenantId; }
    public String getCompanyName() { return companyName; }
    public String getTenantEmail() { return tenantEmail; }
    public String getTermsVersion() { return termsVersion; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public BigDecimal getRevenueSharePct() { return revenueSharePct; }
    public SettlementFrequency getSettlementFrequency() { return settlementFrequency; }
    public String getSignedByName() { return signedByName; }
    public String getSignedByEmail() { return signedByEmail; }
    public String getSignedByDesignation() { return signedByDesignation; }
    public Instant getSignedAt() { return signedAt; }
    public AgreementStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
