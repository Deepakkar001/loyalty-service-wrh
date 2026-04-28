package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingAuditLogRepository extends JpaRepository<OnboardingAuditLog, Long> {
    Page<OnboardingAuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
    Page<OnboardingAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    void deleteByTenantId(String tenantId);
}

