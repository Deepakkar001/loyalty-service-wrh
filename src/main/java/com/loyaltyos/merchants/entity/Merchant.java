package com.loyaltyos.merchants.entity;

import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "merchants",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_merchant",
        columnNames = {"tenant_id", "merchant_uid"}
    ),
    indexes = @Index(name = "idx_tenant_stage", columnList = "tenant_id, onboarding_stage")
)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "merchant_uid", nullable = false, length = 128)
    private String merchantUid;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "category", length = 128)
    private String category;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "bank_details_vault_ref")
    private String bankDetailsVaultRef;

    @Column(name = "tax_id", length = 128)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_stage", nullable = false, length = 32)
    private MerchantOnboardingStage onboardingStage = MerchantOnboardingStage.REGISTRATION;

    @Column(name = "earn_rate_multiplier", nullable = false, precision = 6, scale = 3)
    private BigDecimal earnRateMultiplier = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_cycle", nullable = false, length = 32)
    private SettlementCycle settlementCycle = SettlementCycle.MONTHLY;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "commission_config", columnDefinition = "JSON")
    private String commissionConfig;

    @Column(name = "api_credentials_vault_ref")
    private String apiCredentialsVaultRef;

    @Column(name = "agreement_document_url", length = 512)
    private String agreementDocumentUrl;

    @Column(name = "agreement_accepted_at")
    private Instant agreementAcceptedAt;

    @Column(name = "integration_test_passed_at")
    private Instant integrationTestPassedAt;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Merchant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

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

    public String getBankDetailsVaultRef() { return bankDetailsVaultRef; }
    public void setBankDetailsVaultRef(String bankDetailsVaultRef) { this.bankDetailsVaultRef = bankDetailsVaultRef; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public MerchantOnboardingStage getOnboardingStage() { return onboardingStage; }
    public void setOnboardingStage(MerchantOnboardingStage onboardingStage) { this.onboardingStage = onboardingStage; }

    public BigDecimal getEarnRateMultiplier() { return earnRateMultiplier; }
    public void setEarnRateMultiplier(BigDecimal earnRateMultiplier) { this.earnRateMultiplier = earnRateMultiplier; }

    public SettlementCycle getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(SettlementCycle settlementCycle) { this.settlementCycle = settlementCycle; }

    public String getCommissionConfig() { return commissionConfig; }
    public void setCommissionConfig(String commissionConfig) { this.commissionConfig = commissionConfig; }

    public String getApiCredentialsVaultRef() { return apiCredentialsVaultRef; }
    public void setApiCredentialsVaultRef(String apiCredentialsVaultRef) { this.apiCredentialsVaultRef = apiCredentialsVaultRef; }

    public String getAgreementDocumentUrl() { return agreementDocumentUrl; }
    public void setAgreementDocumentUrl(String agreementDocumentUrl) { this.agreementDocumentUrl = agreementDocumentUrl; }

    public Instant getAgreementAcceptedAt() { return agreementAcceptedAt; }
    public void setAgreementAcceptedAt(Instant agreementAcceptedAt) { this.agreementAcceptedAt = agreementAcceptedAt; }

    public Instant getIntegrationTestPassedAt() { return integrationTestPassedAt; }
    public void setIntegrationTestPassedAt(Instant integrationTestPassedAt) {
        this.integrationTestPassedAt = integrationTestPassedAt;
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isActive() {
        return onboardingStage == MerchantOnboardingStage.ACTIVE;
    }

    public boolean isSuspended() {
        return onboardingStage == MerchantOnboardingStage.SUSPENDED;
    }
}
