package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.domain.entity.TenantConfig;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import com.loyaltyos.onboarding.dto.request.ProgrammeConfigRequest;
import com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.repository.WebhookSubscriptionRepository;
import com.loyaltyos.onboarding.rules.service.RuleCacheService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TenantConfigServiceTest {

    @Test
    void saveLegacyProgrammeConfiguration_invalidTierOrder_throws422() {
        var tenantRepo = mock(TenantOnboardingRepository.class);
        var cfgRepo = mock(TenantConfigRepository.class);
        var tierRepo = mock(TierDefinitionRepository.class);
        var webhookRepo = mock(WebhookSubscriptionRepository.class);
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var stateMachine = mock(OnboardingStateMachine.class);
        var objectMapper = new ObjectMapper();
        var schemaValidator = new ProgrammeConfigSchemaValidator(objectMapper);
        // KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class); // when Kafka re-enabled

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .companyName("Acme")
            .slug("acme")
            .email("x@x.com")
            .passwordHash("hash")
            .businessCategory("RETAIL")
            .onboardingStatus(OnboardingStatus.AGREEMENT_SIGNED)
            .identityMode(IdentityMode.BOTH)
            .subscriptionTier(SubscriptionTier.STANDARD)
            .dataResidencyRegion(DataResidencyRegion.IN)
            .countryCode("IN")
            .build();

        when(tenantRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));
        when(cfgRepo.findByTenantId("t1")).thenReturn(Optional.of(TenantConfig.builder().tenantId("t1").displayName("Old").build()));

        ProgrammeConfigRequest req = new ProgrammeConfigRequest();
        req.setProgrammeName("Prog");
        req.setPointsName("Points");
        req.setPointsSymbol("PTS");
        req.setBaseCurrency("INR");
        req.setBasePointsRate(new BigDecimal("1.0"));
        req.setMinRedemptionPoints(new BigDecimal("100"));
        req.setMaxRedemptionPctPerTxn(new BigDecimal("50"));
        req.setTiersEnabled(true);

        // Non-ascending minPoints
        ProgrammeConfigRequest.TierRequest t1 = new ProgrammeConfigRequest.TierRequest();
        t1.setName("Silver"); t1.setRank(1); t1.setMinPoints(new BigDecimal("100")); t1.setMultiplier(new BigDecimal("1.0"));
        ProgrammeConfigRequest.TierRequest t2 = new ProgrammeConfigRequest.TierRequest();
        t2.setName("Gold"); t2.setRank(2); t2.setMinPoints(new BigDecimal("50")); t2.setMultiplier(new BigDecimal("2.0"));
        req.setTiers(List.of(t1, t2));

        var ruleCache = mock(RuleCacheService.class);
        TenantConfigService svc = new TenantConfigService(
            tenantRepo, cfgRepo, tierRepo, webhookRepo, programmeRepo, programmeConfigRepo, auditRepo, stateMachine,
            objectMapper, schemaValidator, ruleCache
        );

        assertThrows(ProgrammeConfigValidationException.class, () -> svc.saveLegacyProgrammeConfiguration("t1", req));
        verify(tierRepo, never()).saveAll(any());
    }

    @Test
    void saveLegacyProgrammeConfiguration_happyPath_transitionsConfigured() {
        var tenantRepo = mock(TenantOnboardingRepository.class);
        var cfgRepo = mock(TenantConfigRepository.class);
        var tierRepo = mock(TierDefinitionRepository.class);
        var webhookRepo = mock(WebhookSubscriptionRepository.class);
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var stateMachine = mock(OnboardingStateMachine.class);
        var objectMapper = new ObjectMapper();
        var schemaValidator = new ProgrammeConfigSchemaValidator(objectMapper);
        when(programmeRepo.existsByTenantIdAndProgrammeUid("t1", "default")).thenReturn(true);
        when(programmeConfigRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class); // when Kafka re-enabled

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .companyName("Acme")
            .slug("acme")
            .email("x@x.com")
            .passwordHash("hash")
            .businessCategory("RETAIL")
            .onboardingStatus(OnboardingStatus.AGREEMENT_SIGNED)
            .identityMode(IdentityMode.BOTH)
            .subscriptionTier(SubscriptionTier.STANDARD)
            .dataResidencyRegion(DataResidencyRegion.IN)
            .countryCode("IN")
            .build();

        when(tenantRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));
        when(cfgRepo.findByTenantId("t1")).thenReturn(Optional.empty());
        when(cfgRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tierRepo.findByTenantIdOrderByRankOrderAsc("t1")).thenReturn(List.of());

        ProgrammeConfigRequest req = new ProgrammeConfigRequest();
        req.setProgrammeName("Prog");
        req.setPointsName("Points");
        req.setPointsSymbol("PTS");
        req.setBaseCurrency("INR");
        req.setBasePointsRate(new BigDecimal("1.0"));
        req.setMinRedemptionPoints(new BigDecimal("100"));
        req.setMaxRedemptionPctPerTxn(new BigDecimal("50"));
        req.setTiersEnabled(true);
        ProgrammeConfigRequest.TierRequest t1 = new ProgrammeConfigRequest.TierRequest();
        t1.setName("Silver"); t1.setRank(1); t1.setMinPoints(new BigDecimal("0")); t1.setMultiplier(new BigDecimal("1.0"));
        req.setTiers(List.of(t1));

        var ruleCache = mock(RuleCacheService.class);
        TenantConfigService svc = new TenantConfigService(
            tenantRepo, cfgRepo, tierRepo, webhookRepo, programmeRepo, programmeConfigRepo, auditRepo, stateMachine,
            objectMapper, schemaValidator, ruleCache
        );

        svc.saveLegacyProgrammeConfiguration("t1", req);
        verify(ruleCache).invalidateProgramme("t1", "default");
        verify(stateMachine).transition(eq(tenant), eq(OnboardingStatus.CONFIGURED), eq("t1"), eq("TENANT"));
        verify(tenantRepo).save(eq(tenant));
    }
}
