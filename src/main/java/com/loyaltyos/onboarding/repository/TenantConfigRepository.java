package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantConfigRepository extends JpaRepository<TenantConfig, Long> {
    Optional<TenantConfig> findByTenantId(String tenantId);
    boolean existsByTenantId(String tenantId);
}

