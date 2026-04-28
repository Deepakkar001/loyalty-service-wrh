package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantOnboardingRepository extends JpaRepository<TenantOnboarding, Long> {
    Optional<TenantOnboarding> findByTenantId(String tenantId);
    Optional<TenantOnboarding> findByEmail(String email);
    Optional<TenantOnboarding> findBySlug(String slug);
    Optional<TenantOnboarding> findByEmailVerificationToken(String token);
    boolean existsByEmail(String email);
    boolean existsBySlug(String slug);
    boolean existsByTenantId(String tenantId);
    boolean existsByTenantIdAndOnboardingStatus(String tenantId, OnboardingStatus status);

    long countByOnboardingStatus(OnboardingStatus status);
    long countByCreatedAtAfter(Instant after);
    List<TenantOnboarding> findAllByOrderByCreatedAtDesc();

    List<TenantOnboarding> findByEmailVerifiedFalseAndOnboardingStatusAndCreatedAtBefore(OnboardingStatus status, Instant before);
}

