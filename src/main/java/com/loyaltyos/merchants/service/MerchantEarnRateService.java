package com.loyaltyos.merchants.service;

import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantEarnRateService {

    private final MerchantRepository merchantRepository;
    private final MerchantProperties merchantProperties;

    public MerchantEarnRateService(MerchantRepository merchantRepository, MerchantProperties merchantProperties) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveMultiplier(String tenantId, String merchantId) {
        if (!merchantProperties.isEnabled() || merchantId == null || merchantId.isBlank()) {
            return BigDecimal.ONE;
        }
        return merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantId.trim())
            .filter(m -> m.getOnboardingStage() == MerchantOnboardingStage.ACTIVE)
            .map(m -> m.getEarnRateMultiplier() != null ? m.getEarnRateMultiplier() : BigDecimal.ONE)
            .orElse(BigDecimal.ONE);
    }
}
