package com.loyaltyos.rewards.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.TenantConfig;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.dto.RedemptionLimits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgrammeRedemptionConfigResolverTest {

    @Mock
    private ProgrammeService programmeService;

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProgrammeRedemptionConfigResolver resolver;

    @Test
    void resolve_prefersCanonicalConfigOverLegacy() {
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson("{\"minRedemptionPoints\":50,\"maxRedemptionPctPerTxn\":25}");
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        RedemptionLimits limits = resolver.resolve("t1", "default");
        assertThat(limits.minRedemptionPoints()).isEqualByComparingTo("50");
        assertThat(limits.maxRedemptionPctPerTxn()).isEqualByComparingTo("25");
    }

    @Test
    void resolve_fallsBackToLegacyWhenCanonicalEmpty() {
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(null);

        TenantConfig legacy = new TenantConfig();
        legacy.setFeatureFlags("{\"programme\":{\"minRedemptionPoints\":100}}");
        when(tenantConfigRepository.findByTenantId("t1")).thenReturn(Optional.of(legacy));

        RedemptionLimits limits = resolver.resolve("t1", "default");
        assertThat(limits.minRedemptionPoints()).isEqualByComparingTo("100");
        assertThat(limits.maxRedemptionPctPerTxn()).isNull();
    }

    @Test
    void resolve_returnsNoneWhenNoConfig() {
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(null);
        when(tenantConfigRepository.findByTenantId("t1")).thenReturn(Optional.empty());

        RedemptionLimits limits = resolver.resolve("t1", "default");
        assertThat(limits.minRedemptionPoints()).isNull();
        assertThat(limits.maxRedemptionPctPerTxn()).isNull();
    }
}
