package com.loyaltyos.merchants.service;

import com.loyaltyos.integration.security.IntegrationHmacVerifier;
import com.loyaltyos.merchants.dto.MerchantApiKeyResponse;
import com.loyaltyos.merchants.entity.MerchantApiKey;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantApiKeyRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MerchantApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;

    public MerchantApiKeyService(MerchantApiKeyRepository apiKeyRepository, MerchantRepository merchantRepository) {
        this.apiKeyRepository = Objects.requireNonNull(apiKeyRepository, "apiKeyRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
    }

    @Transactional
    public MerchantApiKeyResponse createKey(String tenantId, String merchantUid, String name, String environment) {
        merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));

        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        String apiKey = "mch_" + HexFormat.of().formatHex(raw);
        String hash = IntegrationHmacVerifier.sha256Hex(apiKey);
        String prefix = apiKey.substring(0, Math.min(12, apiKey.length()));

        MerchantApiKey row = new MerchantApiKey();
        row.setKeyUid(UUID.randomUUID().toString());
        row.setTenantId(tenantId);
        row.setMerchantUid(merchantUid);
        row.setKeyHash(hash);
        row.setKeyPrefix(prefix);
        row.setName(name != null && !name.isBlank() ? name.trim() : "Merchant POS key");
        row.setEnvironment(environment != null && !environment.isBlank() ? environment.trim() : "sandbox");
        row.setActive(true);
        apiKeyRepository.save(row);

        MerchantApiKeyResponse response = toResponse(row);
        response.setApiKey(apiKey);
        return response;
    }

    @Transactional(readOnly = true)
    public List<MerchantApiKeyResponse> listKeys(String tenantId, String merchantUid) {
        return apiKeyRepository.findByTenantIdAndMerchantUidOrderByCreatedAtDesc(tenantId, merchantUid)
            .stream()
            .map(MerchantApiKeyService::toResponse)
            .toList();
    }

    @Transactional
    public void revokeKey(String tenantId, String merchantUid, String keyUid) {
        for (MerchantApiKey key : apiKeyRepository.findByTenantIdAndMerchantUidOrderByCreatedAtDesc(tenantId, merchantUid)) {
            if (keyUid.equals(key.getKeyUid())) {
                key.setActive(false);
                apiKeyRepository.save(key);
                return;
            }
        }
        throw new IllegalArgumentException("API key not found: " + keyUid);
    }

    private static MerchantApiKeyResponse toResponse(MerchantApiKey key) {
        MerchantApiKeyResponse r = new MerchantApiKeyResponse();
        r.setKeyUid(key.getKeyUid());
        r.setKeyPrefix(key.getKeyPrefix());
        r.setName(key.getName());
        r.setEnvironment(key.getEnvironment());
        r.setActive(key.isActive());
        r.setCreatedAt(key.getCreatedAt());
        return r;
    }
}
