package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.TenantConfig;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.domain.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.dto.response.GoLiveActivateResponse;
import com.loyaltyos.onboarding.dto.response.GoLiveChecklistResponse;
// import com.loyaltyos.onboarding.event.TenantActivatedEvent; // with Kafka publish
import com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.repository.WebhookSubscriptionRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.kafka.core.KafkaTemplate; // re-enable with Kafka
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class GoLiveService {

    private static final Logger log = LoggerFactory.getLogger(GoLiveService.class);

    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final TenantAgreementRepository tenantAgreementRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final TierDefinitionRepository tierDefinitionRepository;
    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final OnboardingStateMachine stateMachine;
    // private final KafkaTemplate<String, Object> kafkaTemplate; // re-enable with Kafka

    public GoLiveService(
        TenantOnboardingRepository tenantOnboardingRepository,
        TenantAgreementRepository tenantAgreementRepository,
        TenantConfigRepository tenantConfigRepository,
        TierDefinitionRepository tierDefinitionRepository,
        TenantApiKeyRepository tenantApiKeyRepository,
        WebhookSubscriptionRepository webhookSubscriptionRepository,
        OnboardingAuditLogRepository auditLogRepository,
        OnboardingStateMachine stateMachine
    ) {
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.tenantAgreementRepository = Objects.requireNonNull(tenantAgreementRepository, "tenantAgreementRepository");
        this.tenantConfigRepository = Objects.requireNonNull(tenantConfigRepository, "tenantConfigRepository");
        this.tierDefinitionRepository = Objects.requireNonNull(tierDefinitionRepository, "tierDefinitionRepository");
        this.tenantApiKeyRepository = Objects.requireNonNull(tenantApiKeyRepository, "tenantApiKeyRepository");
        this.webhookSubscriptionRepository = Objects.requireNonNull(webhookSubscriptionRepository, "webhookSubscriptionRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
    }

    @Transactional(readOnly = true)
    public GoLiveChecklistResponse getChecklist(String tenantId) {
        tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        var latestAgreement = tenantAgreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId).orElse(null);
        boolean agreementApproved = latestAgreement != null && latestAgreement.getStatus() == AgreementStatus.APPROVED;

        boolean configExists = tenantConfigRepository.existsByTenantId(tenantId);
        boolean tiersExist = tierDefinitionRepository.countByTenantId(tenantId) > 0;
        boolean prodKeyExists = !tenantApiKeyRepository.findByTenantIdAndEnvironmentAndStatus(
            tenantId, ApiKeyEnvironment.PRODUCTION, ApiKeyStatus.ACTIVE).isEmpty();
        boolean webhookConfigured = webhookSubscriptionRepository.countByTenantIdAndActiveTrue(tenantId) > 0;

        boolean canGoLive = agreementApproved && configExists && tiersExist && prodKeyExists;

        List<GoLiveChecklistResponse.Item> items = new ArrayList<>();
        items.add(item("Agreement approved by admin", agreementApproved, true, null));
        items.add(item("Programme configured (tenant_config)", configExists, true, null));
        items.add(item("At least 1 tier defined", tiersExist, true, null));
        items.add(item("Production API credentials generated", prodKeyExists, true, "Generate production keys in Integrate"));
        items.add(item("Webhook URL configured", webhookConfigured, false, null));

        return GoLiveChecklistResponse.builder()
            .canGoLive(canGoLive)
            .items(items)
            .build();
    }

    @Transactional
    public GoLiveActivateResponse activate(String tenantId) {
        TenantOnboarding tenant = tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        GoLiveChecklistResponse checklist = getChecklist(tenantId);
        if (!checklist.isCanGoLive()) {
            Map<String, String> fields = Map.of("goLive", "Required checklist items are incomplete");
            throw new ProgrammeConfigValidationException("Go-live prerequisites not met", fields);
        }

        // Enforce status transition
        if (tenant.getOnboardingStatus() != OnboardingStatus.SANDBOX_TESTING) {
            // Must complete integration phase first
            throw new com.loyaltyos.onboarding.exception.InvalidStateException(
                "Complete integration before activation",
                tenant.getOnboardingStatus().name(),
                OnboardingStatus.SANDBOX_TESTING.name()
            );
        }

        TenantConfig cfg = tenantConfigRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        tenant.setActivatedAt(Instant.now());
        stateMachine.transition(tenant, OnboardingStatus.ACTIVE, tenantId, "TENANT");
        tenantOnboardingRepository.save(tenant);

        cfg.setStatus(com.loyaltyos.onboarding.domain.enums.TenantStatus.ACTIVE);
        tenantConfigRepository.save(cfg);

        auditLogRepository.save(OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("TENANT_ACTIVATED")
            .actorId(tenantId)
            .actorRole("TENANT")
            .afterState(Map.of("activatedAt", tenant.getActivatedAt().toString()))
            .build());

        // --- Kafka publish (disabled) — topic platform.config.updates; downstream cache warmers ---
        // TenantActivatedEvent event = TenantActivatedEvent.builder()
        //     .tenantId(tenantId)
        //     .slug(tenant.getSlug())
        //     .companyName(tenant.getCompanyName())
        //     .identityMode(tenant.getIdentityMode())
        //     .subscriptionTier(tenant.getSubscriptionTier())
        //     .dataResidencyRegion(tenant.getDataResidencyRegion())
        //     .businessCategory(tenant.getBusinessCategory())
        //     .maxActiveRules(cfg.getMaxActiveRules() == null ? 50 : cfg.getMaxActiveRules())
        //     .enabledFeatures(List.of())
        //     .activatedAt(tenant.getActivatedAt())
        //     .activatedByAdminId(null)
        //     .build();
        // kafkaTemplate.send("platform.config.updates", tenantId, event);

        log.info("Tenant activated (Kafka publish skipped) tenantId={}", tenantId);

        return GoLiveActivateResponse.builder()
            .tenantId(tenantId)
            .activatedAt(tenant.getActivatedAt())
            .message("Your programme is now live.")
            .build();
    }

    private static GoLiveChecklistResponse.Item item(String name, boolean ok, boolean required, String details) {
        return GoLiveChecklistResponse.Item.builder()
            .item(name)
            .status(ok ? "COMPLETE" : (required ? "MISSING" : "PENDING"))
            .required(required)
            .details(ok ? null : details)
            .build();
    }
}

