package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.dto.response.AdminDashboardStats;
import com.loyaltyos.onboarding.dto.response.AdminTenantDetail;
import com.loyaltyos.onboarding.dto.response.AdminTenantListItem;
import com.loyaltyos.onboarding.dto.response.AuditLogItem;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantContactRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final TenantOnboardingRepository tenantRepository;
    private final TenantAgreementRepository agreementRepository;
    private final TenantContactRepository contactRepository;
    private final OnboardingAuditLogRepository auditLogRepository;

    public AdminDashboardService(
        TenantOnboardingRepository tenantRepository,
        TenantAgreementRepository agreementRepository,
        TenantContactRepository contactRepository,
        OnboardingAuditLogRepository auditLogRepository
    ) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
    }

    public AdminDashboardStats getStats() {
        long total = tenantRepository.count();

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        return AdminDashboardStats.builder()
                .totalTenants(total)
                .pendingEmailVerification(tenantRepository.countByOnboardingStatus(OnboardingStatus.PENDING_EMAIL_VERIFICATION))
                .emailVerified(tenantRepository.countByOnboardingStatus(OnboardingStatus.EMAIL_VERIFIED))
                .agreementPending(tenantRepository.countByOnboardingStatus(OnboardingStatus.AGREEMENT_PENDING)
                        + tenantRepository.countByOnboardingStatus(OnboardingStatus.AGREEMENT_SIGNED))
                .agreementSigned(tenantRepository.countByOnboardingStatus(OnboardingStatus.AGREEMENT_SIGNED))
                .activeTenants(tenantRepository.countByOnboardingStatus(OnboardingStatus.ACTIVE))
                .suspendedTenants(tenantRepository.countByOnboardingStatus(OnboardingStatus.SUSPENDED))
                .terminatedTenants(tenantRepository.countByOnboardingStatus(OnboardingStatus.TERMINATED))
                .totalAgreements(agreementRepository.count())
                .pendingApprovalAgreements(agreementRepository.countByStatus(AgreementStatus.PENDING_APPROVAL))
                .approvedAgreements(agreementRepository.countByStatus(AgreementStatus.APPROVED))
                .rejectedAgreements(agreementRepository.countByStatus(AgreementStatus.REJECTED))
                .registrationsToday(tenantRepository.countByCreatedAtAfter(startOfToday))
                .registrationsThisWeek(tenantRepository.countByCreatedAtAfter(startOfWeek))
                .registrationsThisMonth(tenantRepository.countByCreatedAtAfter(startOfMonth))
                .build();
    }

    public List<AdminTenantListItem> listAllTenants() {
        List<TenantOnboarding> tenants = tenantRepository.findAllByOrderByCreatedAtDesc();
        return tenants.stream().map(t -> {
            String latestAgreementStatus = agreementRepository
                    .findTopByTenantIdOrderByCreatedAtDesc(t.getTenantId())
                    .map(a -> a.getStatus().name())
                    .orElse(null);

            return AdminTenantListItem.builder()
                    .tenantId(t.getTenantId())
                    .companyName(t.getCompanyName())
                    .slug(t.getSlug())
                    .email(t.getEmail())
                    .businessCategory(t.getBusinessCategory())
                    .countryCode(t.getCountryCode())
                    .onboardingStatus(t.getOnboardingStatus())
                    .identityMode(t.getIdentityMode())
                    .dataResidencyRegion(t.getDataResidencyRegion())
                    .subscriptionTier(t.getSubscriptionTier())
                    .emailVerified(Boolean.TRUE.equals(t.getEmailVerified()))
                    .latestAgreementStatus(latestAgreementStatus)
                    .createdAt(t.getCreatedAt())
                    .activatedAt(t.getActivatedAt())
                    .build();
        }).toList();
    }

    public AdminTenantDetail getTenantDetail(String tenantId) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        Map<String, String> approvalNotesByAgreementUid = new java.util.HashMap<>();
        for (var l : auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 200)).getContent()) {
            if (!"AGREEMENT_APPROVED".equals(l.getAction())) {
                continue;
            }
            var after = l.getAfterState();
            if (after == null) {
                continue;
            }
            Object au = after.get("agreementUid");
            if (au == null) {
                continue;
            }
            String uid = String.valueOf(au).trim();
            if (uid.isBlank() || approvalNotesByAgreementUid.containsKey(uid)) {
                continue; // keep newest (first due to ordering)
            }
            Object an = after.get("approvalNotes");
            String notes = an == null ? null : String.valueOf(an).trim();
            approvalNotesByAgreementUid.put(uid, (notes == null || notes.isBlank()) ? null : notes);
        }

        var contacts = contactRepository.findByTenantId(tenantId).stream()
                .map(c -> AdminTenantDetail.TenantContactItem.builder()
                        .contactName(c.getName())
                        .contactEmail(c.getEmail())
                        .contactPhone(c.getPhone())
                        .designation(c.getDesignation())
                        .role(c.getRole().name())
                        .build())
                .toList();

        var agreements = agreementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(a -> AdminTenantDetail.AgreementHistoryItem.builder()
                        .agreementUid(a.getAgreementUid())
                        .termsVersion(a.getTermsVersion())
                        .effectiveDate(a.getEffectiveDate().toString())
                        .revenueSharePct(a.getRevenueSharePct().doubleValue())
                        .settlementFrequency(a.getSettlementFrequency().name())
                        .signedByName(a.getSignedByName())
                        .signedByEmail(a.getSignedByEmail())
                        .signedByDesignation(a.getSignedByDesignation())
                        .signedAt(a.getSignedAt())
                        .status(a.getStatus().name())
                        .approvedByAdminId(a.getApprovedByAdminId())
                        .approvedAt(a.getApprovedAt())
                        .rejectionReason(a.getRejectionReason())
                        .approvalNotes(approvalNotesByAgreementUid.get(a.getAgreementUid()))
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        return AdminTenantDetail.builder()
                .tenantId(tenant.getTenantId())
                .companyName(tenant.getCompanyName())
                .slug(tenant.getSlug())
                .email(tenant.getEmail())
                .websiteUrl(tenant.getWebsiteUrl())
                .timezone(tenant.getTimezone())
                .countryCode(tenant.getCountryCode())
                .businessCategory(tenant.getBusinessCategory())
                .onboardingStatus(tenant.getOnboardingStatus())
                .identityMode(tenant.getIdentityMode())
                .dataResidencyRegion(tenant.getDataResidencyRegion())
                .subscriptionTier(tenant.getSubscriptionTier())
                .emailVerified(Boolean.TRUE.equals(tenant.getEmailVerified()))
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .activatedAt(tenant.getActivatedAt())
                .suspendedAt(tenant.getSuspendedAt())
                .terminatedAt(tenant.getTerminatedAt())
                .contacts(contacts)
                .agreements(agreements)
                .build();
    }

    public List<AuditLogItem> getRecentAuditLogs(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(log -> AuditLogItem.builder()
                        .id(log.getId())
                        .tenantId(log.getTenantId())
                        .action(log.getAction())
                        .actorId(log.getActorId())
                        .actorRole(log.getActorRole())
                        .beforeState(log.getBeforeState())
                        .afterState(log.getAfterState())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    public List<AuditLogItem> getTenantAuditLogs(String tenantId, int limit) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(log -> AuditLogItem.builder()
                        .id(log.getId())
                        .tenantId(log.getTenantId())
                        .action(log.getAction())
                        .actorId(log.getActorId())
                        .actorRole(log.getActorRole())
                        .beforeState(log.getBeforeState())
                        .afterState(log.getAfterState())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    public List<PendingAgreementListItemInternal> listAllAgreements() {
        return agreementRepository.findAll().stream().map(a -> {
            String companyName = tenantRepository.findByTenantId(a.getTenantId())
                    .map(TenantOnboarding::getCompanyName).orElse("Unknown");
            String tenantEmail = tenantRepository.findByTenantId(a.getTenantId())
                    .map(TenantOnboarding::getEmail).orElse("Unknown");

            return new PendingAgreementListItemInternal(
                    a.getAgreementUid(), a.getTenantId(), companyName, tenantEmail,
                    a.getTermsVersion(), a.getEffectiveDate().toString(),
                    a.getRevenueSharePct().doubleValue(), a.getSettlementFrequency().name(),
                    a.getSignedByName(), a.getSignedByEmail(), a.getSignedByDesignation(),
                    a.getSignedAt(), a.getStatus().name(),
                    a.getApprovedByAdminId(), a.getApprovedAt(), a.getRejectionReason(),
                    a.getCreatedAt()
            );
        }).toList();
    }

    public record PendingAgreementListItemInternal(
            String agreementUid, String tenantId, String companyName, String tenantEmail,
            String termsVersion, String effectiveDate,
            double revenueSharePct, String settlementFrequency,
            String signedByName, String signedByEmail, String signedByDesignation,
            Instant signedAt, String status,
            String approvedByAdminId, Instant approvedAt, String rejectionReason,
            Instant createdAt
    ) {}
}
