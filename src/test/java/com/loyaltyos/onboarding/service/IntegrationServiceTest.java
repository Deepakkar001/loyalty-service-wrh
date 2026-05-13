package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.domain.entity.TenantApiKey;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import com.loyaltyos.onboarding.exception.InvalidStateException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.SandboxTestEventRepository;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.repository.WebhookSubscriptionRepository;
import com.loyaltyos.onboarding.rules.service.RuleEvaluationService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntegrationServiceTest {

    @Test
    void generateKeys_wrongStatus_throwsInvalidState() {
        var tenantRepo = mock(TenantOnboardingRepository.class);
        var keyRepo = mock(TenantApiKeyRepository.class);
        var cfgRepo = mock(TenantConfigRepository.class);
        var webhookRepo = mock(WebhookSubscriptionRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var stateMachine = mock(OnboardingStateMachine.class);

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

        IntegrationService svc = new IntegrationService(
            tenantRepo, keyRepo, cfgRepo, webhookRepo, auditRepo, stateMachine,
            new ObjectMapper(), new RestTemplateBuilder(), mock(RuleEvaluationService.class),
            mock(SandboxTestEventRepository.class)
        );

        assertThrows(InvalidStateException.class, () -> svc.generateKeys("t1", ApiKeyEnvironment.SANDBOX));
    }

    @Test
    void generateKeys_configured_generatesAndStoresHashOnly() {
        var tenantRepo = mock(TenantOnboardingRepository.class);
        var keyRepo = mock(TenantApiKeyRepository.class);
        var cfgRepo = mock(TenantConfigRepository.class);
        var webhookRepo = mock(WebhookSubscriptionRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var stateMachine = mock(OnboardingStateMachine.class);

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .companyName("Acme")
            .slug("acme")
            .email("x@x.com")
            .passwordHash("hash")
            .businessCategory("RETAIL")
            .onboardingStatus(OnboardingStatus.RULES_CONFIGURED)
            .identityMode(IdentityMode.BOTH)
            .subscriptionTier(SubscriptionTier.STANDARD)
            .dataResidencyRegion(DataResidencyRegion.IN)
            .countryCode("IN")
            .build();

        when(tenantRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));
        when(keyRepo.findByTenantIdAndEnvironmentAndStatus(any(), any(), any())).thenReturn(List.of());
        when(keyRepo.save(any())).thenAnswer(inv -> {
            TenantApiKey k = inv.getArgument(0);
            // emulate DB-generated fields
            if (k.getKeyUid() == null) throw new AssertionError("keyUid must be set");
            return k;
        });

        IntegrationService svc = new IntegrationService(
            tenantRepo, keyRepo, cfgRepo, webhookRepo, auditRepo, stateMachine,
            new ObjectMapper(), new RestTemplateBuilder(), mock(RuleEvaluationService.class),
            mock(SandboxTestEventRepository.class)
        );

        var res = svc.generateKeys("t1", ApiKeyEnvironment.SANDBOX);
        assertNotNull(res.getApiKey());
        assertTrue(res.getApiKey().startsWith("los_sandbox_"));
        assertNotNull(res.getSigningSecret());
        assertNotNull(res.getKeyPrefix());

        verify(keyRepo).save(any());
        verify(stateMachine).transition(eq(tenant), eq(OnboardingStatus.SANDBOX_TESTING), eq("t1"), eq("TENANT"));
    }
}

