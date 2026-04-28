package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.TenantApiKey;
import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.domain.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, Long> {
    Optional<TenantApiKey> findByKeyHash(String keyHash);
    List<TenantApiKey> findByTenantIdAndEnvironmentAndStatus(
        String tenantId, ApiKeyEnvironment environment, ApiKeyStatus status);
}

