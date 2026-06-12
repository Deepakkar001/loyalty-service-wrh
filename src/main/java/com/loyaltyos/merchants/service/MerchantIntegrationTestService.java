package com.loyaltyos.merchants.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.merchants.dto.MerchantIntegrationTestRequest;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.onboarding.dto.SandboxValidateEventRequest;
import com.loyaltyos.onboarding.exception.InvalidStateException;
import com.loyaltyos.onboarding.service.IntegrationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantIntegrationTestService {

    private final MerchantRepository merchantRepository;
    private final MerchantOnboardingService onboardingService;
    private final IntegrationService integrationService;
    private final ObjectMapper objectMapper;

    public MerchantIntegrationTestService(
        MerchantRepository merchantRepository,
        MerchantOnboardingService onboardingService,
        IntegrationService integrationService,
        ObjectMapper objectMapper
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.onboardingService = Objects.requireNonNull(onboardingService, "onboardingService");
        this.integrationService = Objects.requireNonNull(integrationService, "integrationService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional
    public MerchantResponse runSandboxTest(
        String tenantId,
        String merchantUid,
        MerchantIntegrationTestRequest request,
        String actorEmail
    ) {
        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));

        if (merchant.getOnboardingStage() != MerchantOnboardingStage.CONFIGURATION) {
            throw new InvalidStateException(
                "Merchant must be in CONFIGURATION stage for integration test",
                merchant.getOnboardingStage().name(),
                MerchantOnboardingStage.CONFIGURATION.name()
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", request.getEventType());
        payload.put("customerId", request.getCustomerId());
        payload.put("amount", request.getAmount());
        payload.put("transactionId", "mch_test_" + UUID.randomUUID());
        payload.put("timestamp", Instant.now().toString());
        payload.put("merchantId", merchantUid);
        payload.put("channel", request.getChannel() != null ? request.getChannel() : "sandbox");

        SandboxValidateEventRequest sandbox = new SandboxValidateEventRequest();
        try {
            sandbox.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to build sandbox payload");
        }

        integrationService.validateSandboxEvent(tenantId, sandbox);

        return onboardingService.completeIntegrationTest(tenantId, merchantUid, actorEmail);
    }
}
