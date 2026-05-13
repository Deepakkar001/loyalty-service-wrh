package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantContactRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class OnboardingDeletionService {

    private final TenantOnboardingRepository tenantRepository;
    private final TenantContactRepository contactRepository;
    private final TenantAgreementRepository agreementRepository;

    public OnboardingDeletionService(
        TenantOnboardingRepository tenantRepository,
        TenantContactRepository contactRepository,
        TenantAgreementRepository agreementRepository
    ) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository");
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
    }

    /**
     * Centralized deletion for onboarding-owned data.
     * Add any future onboarding-owned tables here to avoid orphan rows.
     */
    @Transactional
    public void deleteTenantOnboardingData(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return;
        contactRepository.deleteByTenantId(tenantId);
        agreementRepository.deleteByTenantId(tenantId);
        // NOTE: onboarding_audit_log is immutable by design (DELETE is blocked by DB trigger).
        // We keep audit events even if a pending registration is cleaned up.
        tenantRepository.findByTenantId(tenantId).ifPresent(tenantRepository::delete);
    }
}

