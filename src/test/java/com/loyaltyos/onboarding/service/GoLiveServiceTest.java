package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.TenantConfig;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.domain.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import com.loyaltyos.onboarding.exception.InvalidStateException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.repository.WebhookSubscriptionRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoLiveServiceTest {

    @Test
    void getChecklist_allRequiredComplete_canGoLiveTrue() {
        var tenantRepo = mock(TenantOnboardingRepository.class);
        var agreementRepo = mock(TenantAgreementRepository.class);
        var cfgRepo = mock(TenantConfigRepository.class);
        var tierRepo = mock(TierDefinitionRepository.class);
        var keyRepo = mock(TenantApiKeyRepository.class);
        var webhookRepo = mock(WebhookSubscriptionRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var stateMachine = mock(OnboardingStateMachine.class);
        // KafkaTemplate kafka = mock(KafkaTemplate.class); // when Kafka re-enabled

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .companyName("Acme")
            .slug("acme")
            .email("x@x.com")
            .passwordHash("hash")
            .businessCategory("RETAIL")
            .onboardingStatus(OnboardingStatus.SANDBOX_TESTING)
            .identityMode(IdentityMode.BOTH)
            .subscriptionTier(SubscriptionTier.STANDARD)
            .dataResidencyRegion(DataResidencyRegion.IN)
            .countryCode("IN")
            .build();

        when(tenantRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));
        var agreement = new com.loyaltyos.onboarding.domain.entity.TenantAgreement();
        agreement.setStatus(AgreementStatus.APPROVED);
        when(agreementRepo.findTopByTenantIdOrderByCreatedAtDesc("t1")).thenReturn(Optional.of(agreement));
        when(cfgRepo.existsByTenantId("t1")).thenReturn(true);
        when(tierRepo.countByTenantId("t1")).thenReturn(1L);
        when(keyRepo.findByTenantIdAndEnvironmentAndStatus("t1", ApiKeyEnvironment.PRODUCTION, ApiKeyStatus.ACTIVE))
            .thenReturn(List.of(new com.loyaltyos.onboarding.domain.entity.TenantApiKey()));
        when(webhookRepo.countByTenantIdAndActiveTrue("t1")).thenReturn(0L);

        GoLiveService svc = new GoLiveService(
            tenantRepo, agreementRepo, cfgRepo, tierRepo, keyRepo, webhookRepo, auditRepo, stateMachine
        );

        var checklist = svc.getChecklist("t1");
        assertTrue(checklist.isCanGoLive());
    }

    @Test
    void activate_wrongOnboardingStatus_throwsInvalidState() {
        var tenantRepo = mock(TenantOnboardingRepository.class);
        var agreementRepo = mock(TenantAgreementRepository.class);
        var cfgRepo = mock(TenantConfigRepository.class);
        var tierRepo = mock(TierDefinitionRepository.class);
        var keyRepo = mock(TenantApiKeyRepository.class);
        var webhookRepo = mock(WebhookSubscriptionRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var stateMachine = mock(OnboardingStateMachine.class);
        // KafkaTemplate kafka = mock(KafkaTemplate.class); // when Kafka re-enabled

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .companyName("Acme")
            .slug("acme")
            .email("x@x.com")
            .passwordHash("hash")
            .businessCategory("RETAIL")
            .onboardingStatus(OnboardingStatus.CONFIGURED)
            .identityMode(IdentityMode.BOTH)
            .subscriptionTier(SubscriptionTier.STANDARD)
            .dataResidencyRegion(DataResidencyRegion.IN)
            .countryCode("IN")
            .build();

        when(tenantRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));
        var agreement = new com.loyaltyos.onboarding.domain.entity.TenantAgreement();
        agreement.setStatus(AgreementStatus.APPROVED);
        when(agreementRepo.findTopByTenantIdOrderByCreatedAtDesc("t1")).thenReturn(Optional.of(agreement));
        when(cfgRepo.existsByTenantId("t1")).thenReturn(true);
        when(tierRepo.countByTenantId("t1")).thenReturn(1L);
        when(keyRepo.findByTenantIdAndEnvironmentAndStatus("t1", ApiKeyEnvironment.PRODUCTION, ApiKeyStatus.ACTIVE))
            .thenReturn(List.of(new com.loyaltyos.onboarding.domain.entity.TenantApiKey()));

        when(cfgRepo.findByTenantId("t1")).thenReturn(Optional.of(TenantConfig.builder().tenantId("t1").displayName("Acme").build()));

        GoLiveService svc = new GoLiveService(
            tenantRepo, agreementRepo, cfgRepo, tierRepo, keyRepo, webhookRepo, auditRepo, stateMachine
        );

        assertThrows(InvalidStateException.class, () -> svc.activate("t1"));
        // verify(kafka, never()).send(eq("platform.config.updates"), any(), any()); // when Kafka re-enabled
    }
}

