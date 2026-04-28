package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingRegistrationCleanupJob {

    private final TenantOnboardingRepository tenantRepository;
    private final OnboardingDeletionService deletionService;

    @Value("${app.onboarding.pending-expiry-hours:24}")
    private long pendingExpiryHours;

    /**
     * Production-grade housekeeping: remove abandoned, unverified signups so
     * emails don't stay blocked forever and the onboarding table stays clean.
     *
     * Runs hourly; deletes records older than 24h in PENDING_EMAIL_VERIFICATION.
     */
    @Scheduled(fixedDelayString = "${app.onboarding.cleanup.fixed-delay-ms:3600000}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(pendingExpiryHours, ChronoUnit.HOURS);
        var stale = tenantRepository.findByEmailVerifiedFalseAndOnboardingStatusAndCreatedAtBefore(
            OnboardingStatus.PENDING_EMAIL_VERIFICATION, cutoff
        );
        if (stale.isEmpty()) return;

        for (var t : stale) {
            String tenantId = t.getTenantId();
            try {
                deletionService.deleteTenantOnboardingData(tenantId);
                log.info("Cleaned up stale unverified registration tenantId={} email={}", tenantId, t.getEmail());
            } catch (Exception e) {
                log.warn("Failed to cleanup tenantId={} email={}: {}", tenantId, t.getEmail(), e.getMessage());
            }
        }
    }
}

