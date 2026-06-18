package com.loyaltyos.merchants.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.dto.UpdateMerchantConfigRequest;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantApprovalRequest;
import com.loyaltyos.merchants.enums.MerchantApprovalRequestType;
import com.loyaltyos.merchants.enums.MerchantApprovalStatus;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantApprovalRequestRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.service.statemachine.MerchantOnboardingStateMachine;
import com.loyaltyos.merchants.support.MerchantMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantConfigApprovalService {

    private final MerchantRepository merchantRepository;
    private final MerchantApprovalRequestRepository approvalRequestRepository;
    private final MerchantOnboardingStateMachine stateMachine;
    private final MerchantValidationService validationService;
    private final ObjectMapper objectMapper;

    public MerchantConfigApprovalService(
        MerchantRepository merchantRepository,
        MerchantApprovalRequestRepository approvalRequestRepository,
        MerchantOnboardingStateMachine stateMachine,
        MerchantValidationService validationService,
        ObjectMapper objectMapper
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.approvalRequestRepository = Objects.requireNonNull(
            approvalRequestRepository, "approvalRequestRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.validationService = Objects.requireNonNull(validationService, "validationService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional
    public MerchantResponse submitConfigForApproval(
        String tenantId,
        String merchantUid,
        UpdateMerchantConfigRequest request,
        String actorEmail
    ) {
        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
        if (request.getEarnRateMultiplier() != null) {
            validationService.validateEarnRateMultiplier(request.getEarnRateMultiplier());
        }
        if (request.getCommissionConfigJson() != null) {
            validationService.validateCommissionConfigJson(request.getCommissionConfigJson());
        }
        if (request.getEligibleCategoriesJson() != null) {
            validationService.validateEligibleCategoriesJson(request.getEligibleCategoriesJson());
        }

        MerchantApprovalRequest row = new MerchantApprovalRequest();
        row.setRequestUid(UUID.randomUUID().toString());
        row.setTenantId(tenantId);
        row.setMerchantUid(merchantUid);
        row.setRequestType(MerchantApprovalRequestType.CONFIG_UPDATE);
        row.setRequestedBy(actorEmail != null ? actorEmail : merchantUid);
        row.setRequestedAt(Instant.now());
        row.setStatus(MerchantApprovalStatus.PENDING);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request.getEarnRateMultiplier() != null) {
            payload.put("earnRateMultiplier", request.getEarnRateMultiplier());
        }
        if (request.getSettlementCycle() != null) {
            payload.put("settlementCycle", request.getSettlementCycle());
        }
        if (request.getCommissionConfigJson() != null) {
            payload.put("commissionConfigJson", request.getCommissionConfigJson());
        }
        if (request.getEligibleCategoriesJson() != null) {
            payload.put("eligibleCategoriesJson", request.getEligibleCategoriesJson());
        }
        if (request.getCapabilitiesJson() != null) {
            payload.put("capabilitiesJson", request.getCapabilitiesJson());
        }
        try {
            row.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize config approval payload");
        }
        approvalRequestRepository.save(row);
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse approveConfigRequest(
        String tenantId,
        String merchantUid,
        String requestUid,
        String reviewerEmail
    ) {
        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
        MerchantApprovalRequest request = approvalRequestRepository.findByRequestUid(requestUid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));
        if (request.getStatus() != MerchantApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not pending");
        }
        if (request.getRequestedBy() != null && request.getRequestedBy().equalsIgnoreCase(reviewerEmail)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Maker-checker: approver must differ from submitter"
            );
        }
        applyPayload(merchant, request.getPayloadJson());
        stateMachine.transition(
            merchant,
            MerchantOnboardingStage.CONFIGURATION,
            reviewerEmail,
            "CONFIGURATION_APPROVED",
            null,
            null
        );
        merchantRepository.save(merchant);
        request.setStatus(MerchantApprovalStatus.APPROVED);
        request.setReviewedBy(reviewerEmail);
        request.setReviewedAt(Instant.now());
        approvalRequestRepository.save(request);
        return MerchantMapper.toResponse(merchant);
    }

    private void applyPayload(Merchant merchant, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return;
        }
        try {
            var node = objectMapper.readTree(payloadJson);
            if (node.has("earnRateMultiplier") && !node.get("earnRateMultiplier").isNull()) {
                merchant.setEarnRateMultiplier(node.get("earnRateMultiplier").decimalValue());
            }
            if (node.has("commissionConfigJson") && !node.get("commissionConfigJson").isNull()) {
                merchant.setCommissionConfig(node.get("commissionConfigJson").toString());
            }
            if (node.has("eligibleCategoriesJson") && !node.get("eligibleCategoriesJson").isNull()) {
                merchant.setEligibleCategoriesJson(node.get("eligibleCategoriesJson").toString());
            }
            if (node.has("capabilitiesJson") && !node.get("capabilitiesJson").isNull()) {
                merchant.setCapabilitiesJson(node.get("capabilitiesJson").toString());
            }
            if (node.has("settlementCycle") && !node.get("settlementCycle").isNull()) {
                merchant.setSettlementCycle(
                    com.loyaltyos.merchants.enums.SettlementCycle.valueOf(
                        node.get("settlementCycle").asText().trim().toUpperCase()));
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid config approval payload");
        }
    }
}
