package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.TenantAgreement;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.dto.response.PendingAgreementListItem;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementApprovalService {

    private final TenantAgreementRepository agreementRepository;
    private final TenantOnboardingRepository tenantRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final AgreementNotificationMailer notificationMailer;

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

        auditLogRepository.save(OnboardingAuditLog.builder()
                .tenantId(agreement.getTenantId())
                .action("AGREEMENT_APPROVED")
                .actorId(adminUid)
                .actorRole(adminRole)
                .beforeState(Map.of("agreementStatus", AgreementStatus.PENDING_APPROVAL.name()))
                .afterState(Map.of(
                        "agreementStatus", AgreementStatus.APPROVED.name(),
                        "approvedBy", adminUid,
                        "approvalNotes", approvalNotes != null ? approvalNotes : ""
                ))
                .build());

        log.info("Agreement [{}] for tenant [{}] APPROVED by admin [{}]",
                agreementUid, agreement.getTenantId(), adminUid);

        sendApprovalNotification(agreement);
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

        auditLogRepository.save(OnboardingAuditLog.builder()
                .tenantId(agreement.getTenantId())
                .action("AGREEMENT_REJECTED")
                .actorId(adminUid)
                .actorRole(adminRole)
                .beforeState(Map.of("agreementStatus", AgreementStatus.PENDING_APPROVAL.name()))
                .afterState(Map.of(
                        "agreementStatus", AgreementStatus.REJECTED.name(),
                        "rejectedBy", adminUid,
                        "rejectionReason", rejectionReason
                ))
                .build());

        log.info("Agreement [{}] for tenant [{}] REJECTED by admin [{}]: {}",
                agreementUid, agreement.getTenantId(), adminUid, rejectionReason);

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

    private void sendApprovalNotification(TenantAgreement agreement) {
        tenantRepository.findByTenantId(agreement.getTenantId()).ifPresent(tenant ->
                notificationMailer.sendApprovalEmail(tenant.getEmail(), tenant.getCompanyName()));
    }

    private void sendRejectionNotification(TenantAgreement agreement, String reason) {
        tenantRepository.findByTenantId(agreement.getTenantId()).ifPresent(tenant ->
                notificationMailer.sendRejectionEmail(tenant.getEmail(), tenant.getCompanyName(), reason));
    }
}
