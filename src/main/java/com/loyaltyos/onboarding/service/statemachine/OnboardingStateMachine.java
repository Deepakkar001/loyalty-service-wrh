package com.loyaltyos.onboarding.service.statemachine;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.exception.InvalidStatusTransitionException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingStateMachine {

    private final OnboardingAuditLogRepository auditLogRepository;

    /**
     * Enforces the 6-stage tenant onboarding state machine.
     *
     * Rules:
     * - No stage can be skipped.
     * - TERMINATED is a terminal state with no exits.
     * - Every transition is recorded in onboarding_audit_log.
     * - This class ONLY modifies the tenant's status in memory.
     *   The calling service MUST save the entity AND call this within a @Transactional method.
     */
    public void transition(TenantOnboarding tenant, OnboardingStatus next,
                           String actorId, String actorRole) {

        OnboardingStatus current = tenant.getOnboardingStatus();

        if (!current.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(current, next);
        }

        log.info("Tenant [{}] onboarding status: {} → {}", tenant.getTenantId(), current, next);

        // Record audit BEFORE applying change
        auditLogRepository.save(OnboardingAuditLog.builder()
            .tenantId(tenant.getTenantId())
            .action("STATUS_TRANSITION")
            .actorId(actorId)
            .actorRole(actorRole)
            .beforeState(Map.of("onboardingStatus", current.name()))
            .afterState(Map.of("onboardingStatus", next.name()))
            .build());

        // Apply transition
        tenant.setOnboardingStatus(next);
    }
}

