package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class SetupProgressService {

    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final OnboardingStateMachine stateMachine;

    public SetupProgressService(TenantOnboardingRepository tenantOnboardingRepository, OnboardingStateMachine stateMachine) {
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
    }

    @Transactional
    public void completeRules(String tenantId) {
        TenantOnboarding tenant = tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.getOnboardingStatus() == OnboardingStatus.RULES_CONFIGURED
            || tenant.getOnboardingStatus() == OnboardingStatus.SANDBOX_TESTING
            || tenant.getOnboardingStatus() == OnboardingStatus.ACTIVE) {
            return; // idempotent
        }

        // Only allow completion after configuration is saved.
        if (tenant.getOnboardingStatus() != OnboardingStatus.CONFIGURED) {
            throw new com.loyaltyos.onboarding.exception.InvalidStateException(
                "Tenant must complete programme configuration before completing rules setup",
                tenant.getOnboardingStatus().name(),
                OnboardingStatus.CONFIGURED.name()
            );
        }

        stateMachine.transition(tenant, OnboardingStatus.RULES_CONFIGURED, tenantId, "TENANT");
        tenantOnboardingRepository.save(tenant);
    }
}

