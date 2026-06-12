package com.loyaltyos.merchants.service;

import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.MerchantNotActiveException;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantEventValidationService {

    private final MerchantRepository merchantRepository;
    private final MerchantProperties merchantProperties;

    public MerchantEventValidationService(
        MerchantRepository merchantRepository,
        MerchantProperties merchantProperties
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
    }

    public String resolveMerchantId(IntegrationParsedEvent parsed) {
        if (parsed == null) {
            return null;
        }
        String fromMeta = firstNonBlank(parsed.metadata(), "merchantId", "merchant_id");
        if (fromMeta != null) {
            return fromMeta;
        }
        return firstNonBlank(parsed.schemaPayload(), "merchantId", "merchant_id");
    }

    @Transactional(readOnly = true)
    public Merchant validateMerchantIsActive(String tenantId, String merchantId) {
        if (!merchantProperties.isEnabled()) {
            return null;
        }
        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantId)
            .orElseThrow(() -> new MerchantNotFoundException(merchantId));
        if (merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
            throw new MerchantNotActiveException(
                "Merchant " + merchantId + " is in stage " + merchant.getOnboardingStage() + "; must be ACTIVE");
        }
        return merchant;
    }

    public Map<String, Object> merchantContext(Merchant merchant) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("uid", merchant.getMerchantUid());
        ctx.put("earnRateMultiplier", merchant.getEarnRateMultiplier());
        ctx.put("settlementCycle", merchant.getSettlementCycle().name());
        return ctx;
    }

    private static String firstNonBlank(Map<String, Object> map, String... keys) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }
}
