package com.loyaltyos.merchants.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantEarnRateServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantProperties merchantProperties;

    @InjectMocks private MerchantEarnRateService service;

    @Test
    void returnsOneWhenMerchantDisabled() {
        when(merchantProperties.isEnabled()).thenReturn(false);
        assertThat(service.resolveMultiplier("t1", "m1")).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void returnsMultiplierForActiveMerchant() {
        when(merchantProperties.isEnabled()).thenReturn(true);
        Merchant merchant = new Merchant();
        merchant.setOnboardingStage(MerchantOnboardingStage.ACTIVE);
        merchant.setEarnRateMultiplier(new BigDecimal("1.5"));
        when(merchantRepository.findByTenantIdAndMerchantUid("t1", "m1")).thenReturn(Optional.of(merchant));

        assertThat(service.resolveMultiplier("t1", "m1")).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    void returnsOneWhenMerchantNotActive() {
        when(merchantProperties.isEnabled()).thenReturn(true);
        Merchant merchant = new Merchant();
        merchant.setOnboardingStage(MerchantOnboardingStage.CONFIGURATION);
        merchant.setEarnRateMultiplier(new BigDecimal("2.0"));
        when(merchantRepository.findByTenantIdAndMerchantUid("t1", "m1")).thenReturn(Optional.of(merchant));

        assertThat(service.resolveMultiplier("t1", "m1")).isEqualByComparingTo(BigDecimal.ONE);
    }
}
