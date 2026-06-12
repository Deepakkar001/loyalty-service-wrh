package com.loyaltyos.access.service;

import com.loyaltyos.onboarding.entity.TenantApiKey;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class IntegrationApiKeyLifecycleService {

    private final TenantApiKeyRepository apiKeyRepository;

    public IntegrationApiKeyLifecycleService(TenantApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = Objects.requireNonNull(apiKeyRepository, "apiKeyRepository");
    }

    @Transactional
    public void syncForIntegrationsModule(String tenantId, boolean integrationsEnabled) {
        for (TenantApiKey key : apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)) {
            if (integrationsEnabled) {
                if (key.getStatus() == ApiKeyStatus.SUSPENDED) {
                    key.setStatus(ApiKeyStatus.ACTIVE);
                    apiKeyRepository.save(key);
                }
            } else if (key.getStatus() == ApiKeyStatus.ACTIVE) {
                key.setStatus(ApiKeyStatus.SUSPENDED);
                apiKeyRepository.save(key);
            }
        }
    }
}
