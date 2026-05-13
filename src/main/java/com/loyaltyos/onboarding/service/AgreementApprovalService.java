package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.TenantAgreement;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.dto.response.PendingAgreementListItem;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AgreementApprovalService {

    private static final Logger log = LoggerFactory.getLogger(AgreementApprovalService.class);

    private final TenantAgreementRepository agreementRepository;
    private final TenantOnboardingRepository tenantRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final AgreementNotificationMailer notificationMailer;
    private final OnboardingStateMachine stateMachine;

    public AgreementApprovalService(
        TenantAgreementRepository agreementRepository,
        TenantOnboardingRepository tenantRepository,
        OnboardingAuditLogRepository auditLogRepository,
        AgreementNotificationMailer notificationMailer,
        OnboardingStateMachine stateMachine
    ) {
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.notificationMailer = Objects.requireNonNull(notificationMailer, "notificationMailer");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
    }

    @Transactional(readOnly = true)
    public List<PendingAgreementListItem> listPendingAgreements() {
        List<TenantAgreement> pending = agreementRepository.findByStatus(AgreementStatus.PENDING_APPROVAL);

        return pending.stream().map(a -> {
            String companyName = tenantRepository.findByTenantId(a.getTenantId())
                    .map(TenantOnboarding::getCompanyName)
                    .orElse("Unknown");
            String tenantEmail = tenantRepository.findByTenantId(a.getTenantId())
                    .map(TenantOnboarding::getEmail)
                    .orElse("");

            return PendingAgreementListItem.builder()
                    .agreementUid(a.getAgreementUid())
                    .tenantId(a.getTenantId())
                    .companyName(companyName)
                    .tenantEmail(tenantEmail)
                    .termsVersion(a.getTermsVersion())
                    .effectiveDate(a.getEffectiveDate())
                    .revenueSharePct(a.getRevenueSharePct())
                    .settlementFrequency(a.getSettlementFrequency())
                    .signedByName(a.getSignedByName())
                    .signedByEmail(a.getSignedByEmail())
                    .signedByDesignation(a.getSignedByDesignation())
                    .signedAt(a.getSignedAt())
                    .status(a.getStatus())
                    .createdAt(a.getCreatedAt())
                    .build();
        }).toList();
    }

    @Transactional
    public void approveAgreement(String agreementUid, String adminUid, String adminRole, String approvalNotes) {
        TenantAgreement agreement = agreementRepository.findByAgreementUid(agreementUid)
                .orElseThrow(() -> new TenantNotFoundException("Agreement not found: " + agreementUid));

        if (agreement.getStatus() != AgreementStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Agreement is not pending approval (current: " + agreement.getStatus() + ")");
        }

        makerCheckerGuard(agreement, adminUid);

        agreement.setStatus(AgreementStatus.APPROVED);
        agreement.setApprovedByAdminId(adminUid);
        agreement.setApprovedAt(Instant.now());
        agreementRepository.save(agreement);

        OnboardingAuditLog approvedAudit = OnboardingAuditLog.builder()
                .tenantId(agreement.getTenantId())
                .action("AGREEMENT_APPROVED")
                .actorId(adminUid)
                .actorRole(adminRole)
                .beforeState(Map.of("agreementStatus", AgreementStatus.PENDING_APPROVAL.name()))
                .afterState(Map.of(
                        "agreementUid", agreementUid,
                        "agreementStatus", AgreementStatus.APPROVED.name(),
                        "approvedBy", adminUid,
                        "approvalNotes", approvalNotes != null ? approvalNotes : ""
                ))
                .build();
        auditLogRepository.save(Objects.requireNonNull(approvedAudit, "approvedAudit"));

        log.info("Agreement [{}] for tenant [{}] APPROVED by admin [{}]",
                agreementUid, agreement.getTenantId(), adminUid);

        sendApprovalNotification(agreement, approvalNotes);
    }

    @Transactional
    public void rejectAgreement(String agreementUid, String adminUid, String adminRole, String rejectionReason) {
        TenantAgreement agreement = agreementRepository.findByAgreementUid(agreementUid)
                .orElseThrow(() -> new TenantNotFoundException("Agreement not found: " + agreementUid));

        if (agreement.getStatus() != AgreementStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Agreement is not pending approval (current: " + agreement.getStatus() + ")");
        }

        makerCheckerGuard(agreement, adminUid);

        agreement.setStatus(AgreementStatus.REJECTED);
        agreement.setRejectionReason(rejectionReason);
        agreement.setApprovedByAdminId(adminUid);
        agreementRepository.save(agreement);

        OnboardingAuditLog rejectedAudit = OnboardingAuditLog.builder()
                .tenantId(agreement.getTenantId())
                .action("AGREEMENT_REJECTED")
                .actorId(adminUid)
                .actorRole(adminRole)
                .beforeState(Map.of("agreementStatus", AgreementStatus.PENDING_APPROVAL.name()))
                .afterState(Map.of(
                        "agreementUid", agreementUid,
                        "agreementStatus", AgreementStatus.REJECTED.name(),
                        "rejectedBy", adminUid,
                        "rejectionReason", rejectionReason
                ))
                .build();
        auditLogRepository.save(Objects.requireNonNull(rejectedAudit, "rejectedAudit"));

        log.info("Agreement [{}] for tenant [{}] REJECTED by admin [{}]: {}",
                agreementUid, agreement.getTenantId(), adminUid, rejectionReason);

        // Critical fix: rejection must move tenant to AGREEMENT_REJECTED so they cannot proceed to configuration.
        tenantRepository.findByTenantId(agreement.getTenantId()).ifPresent(tenant -> {
            if (tenant.getOnboardingStatus() == OnboardingStatus.AGREEMENT_SIGNED) {
                stateMachine.transition(tenant, OnboardingStatus.AGREEMENT_REJECTED, adminUid, adminRole);
                tenantRepository.save(tenant);
            }
        });

        sendRejectionNotification(agreement, rejectionReason);
    }

    /**
     * Maker-checker: if the tenant was created by an admin, that same admin cannot approve.
     */
    private void makerCheckerGuard(TenantAgreement agreement, String adminUid) {
        tenantRepository.findByTenantId(agreement.getTenantId()).ifPresent(tenant -> {
            if (adminUid.equals(tenant.getCreatedByAdminId())) {
                throw new IllegalStateException(
                        "Maker-checker violation: the admin who created this tenant cannot approve/reject their agreement.");
            }
        });
    }

    private void sendApprovalNotification(TenantAgreement agreement, String approvalNotes) {
        tenantRepository.findByTenantId(agreement.getTenantId()).ifPresent(tenant ->
                notificationMailer.sendApprovalEmail(tenant.getEmail(), tenant.getCompanyName(), approvalNotes));
    }

    private void sendRejectionNotification(TenantAgreement agreement, String reason) {
        tenantRepository.findByTenantId(agreement.getTenantId()).ifPresent(tenant ->
                notificationMailer.sendRejectionEmail(tenant.getEmail(), tenant.getCompanyName(), reason));
    }
}
