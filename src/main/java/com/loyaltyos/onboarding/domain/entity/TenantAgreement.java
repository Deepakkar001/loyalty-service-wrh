package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.SettlementFrequency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tenant_agreements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
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
    @Builder.Default
    private Integer contractDurationMonths = 12;

    @Column(name = "auto_renewal", nullable = false)
    @Builder.Default
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
    @Builder.Default
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
}

