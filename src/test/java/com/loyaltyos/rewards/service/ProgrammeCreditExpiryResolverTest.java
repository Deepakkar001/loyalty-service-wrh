package com.loyaltyos.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.analytics.service.TierResolver;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.TenantConfig;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.config.RewardEngineProperties;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgrammeCreditExpiryResolverTest {

    private static final Instant EARNED_AT = LocalDate.of(2026, 1, 15).atStartOfDay().toInstant(ZoneOffset.UTC);

    @Mock
    private ProgrammeService programmeService;

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RewardEngineProperties rewardEngineProperties;

    @Mock
    private TierResolver tierResolver;

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @InjectMocks
    private ProgrammeCreditExpiryResolver resolver;

    @BeforeEach
    void setUp() {
        when(pointsLedgerRepository.sumSignedPointsForCustomer(eq("t1"), eq("default"), any()))
            .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void resolveExpiresAt_usesRollingMonthsFromProgrammeConfig() {
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson("""
            {
              "expiry": {
                "model": "ROLLING",
                "rollingMonths": 36,
                "tierExtensionsEnabled": false,
                "processMode": "OVERNIGHT_BATCH"
              }
            }
            """);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        Instant expiresAt = resolver.resolveExpiresAt("t1", "default", EARNED_AT, null);

        assertThat(expiresAt).isEqualTo(EARNED_AT.atZone(ZoneOffset.UTC).plusMonths(36).toInstant());
    }

    @Test
    void resolveExpiresAt_usesFixedDateFromProgrammeConfig() {
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson("""
            {
              "expiry": {
                "model": "FIXED_DATE",
                "fixedDate": "2026-12-31",
                "tierExtensionsEnabled": false,
                "processMode": "OVERNIGHT_BATCH"
              }
            }
            """);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        Instant expiresAt = resolver.resolveExpiresAt("t1", "default", EARNED_AT, null);

        assertThat(expiresAt).isEqualTo(LocalDate.of(2026, 12, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC));
    }

    @Test
    void resolveExpiresAt_addsTierExtensionForRollingModel() {
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson("""
            {
              "expiry": {
                "model": "ROLLING",
                "rollingMonths": 24,
                "tierExtensionsEnabled": true,
                "processMode": "OVERNIGHT_BATCH"
              },
              "tiers": {
                "enabled": true,
                "tiers": [
                  { "tierUid": "gold", "expiryExtensionMonths": 6 }
                ]
              }
            }
            """);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        Instant expiresAt = resolver.resolveExpiresAt("t1", "default", EARNED_AT, "gold");

        assertThat(expiresAt).isEqualTo(EARNED_AT.atZone(ZoneOffset.UTC).plusMonths(30).toInstant());
    }

    @Test
    void resolveExpiresAt_fallsBackToApplicationDefaultWhenConfigMissing() {
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(null);
        when(tenantConfigRepository.findByTenantId("t1")).thenReturn(Optional.empty());
        when(rewardEngineProperties.getDefaultCreditExpiryMonths()).thenReturn(18);

        Instant expiresAt = resolver.resolveExpiresAt("t1", "default", EARNED_AT, null);

        assertThat(expiresAt).isEqualTo(EARNED_AT.atZone(ZoneOffset.UTC).plusMonths(18).toInstant());
    }

    @Test
    void resolveExpiresAt_fallsBackToLegacyTenantConfigForDefaultProgramme() {
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(null);

        TenantConfig legacy = new TenantConfig();
        legacy.setProgrammeConfig("""
            {
              "expiry": {
                "model": "ROLLING",
                "rollingMonths": 12,
                "tierExtensionsEnabled": false,
                "processMode": "OVERNIGHT_BATCH"
              }
            }
            """);
        when(tenantConfigRepository.findByTenantId("t1")).thenReturn(Optional.of(legacy));

        Instant expiresAt = resolver.resolveExpiresAt("t1", "default", EARNED_AT, null);

        assertThat(expiresAt).isEqualTo(EARNED_AT.atZone(ZoneOffset.UTC).plusMonths(12).toInstant());
    }
}
