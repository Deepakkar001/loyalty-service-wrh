package com.loyaltyos.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.service.CampaignOrchestrationService;
import com.loyaltyos.integration.dto.EventIdempotentReplayResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.EventStatusResponse;
import com.loyaltyos.integration.dto.IntegrationEventRequest;
import com.loyaltyos.integration.dto.ValidationResponse;
import com.loyaltyos.integration.entity.IntegrationEventProcessingLog;
import com.loyaltyos.integration.enums.EventProcessingStatus;
import com.loyaltyos.integration.exception.IntegrationApiException;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import com.loyaltyos.integration.security.IntegrationHmacVerifier;
import com.loyaltyos.onboarding.dto.SandboxValidateEventRequest;
import com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException;
import com.loyaltyos.onboarding.service.IntegrationService;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class IntegrationEventService {

    private final CampaignOrchestrationService campaignOrchestrationService;
    private final IntegrationService integrationService;
    private final IntegrationIdempotencyService idempotencyService;
    private final IntegrationResponseMapper responseMapper;
    private final IntegrationEventProcessingLogRepository processingLogRepository;
    private final ObjectMapper objectMapper;

    public IntegrationEventService(
        CampaignOrchestrationService campaignOrchestrationService,
        IntegrationService integrationService,
        IntegrationIdempotencyService idempotencyService,
        IntegrationResponseMapper responseMapper,
        IntegrationEventProcessingLogRepository processingLogRepository,
        ObjectMapper objectMapper
    ) {
        this.campaignOrchestrationService = Objects.requireNonNull(campaignOrchestrationService, "campaignOrchestrationService");
        this.integrationService = Objects.requireNonNull(integrationService, "integrationService");
        this.idempotencyService = Objects.requireNonNull(idempotencyService, "idempotencyService");
        this.responseMapper = Objects.requireNonNull(responseMapper, "responseMapper");
        this.processingLogRepository = Objects.requireNonNull(processingLogRepository, "processingLogRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional
    public Object processEvent(
        String tenantId,
        IntegrationEventRequest request,
        String apiKeyUid,
        String requestBody,
        String requestPayloadHash
    ) {
        Optional<EventProcessingResponse> cached = idempotencyService.getCachedResponse(tenantId, request.getEventId());
        if (cached.isPresent()) {
            return buildIdempotentReplay(request.getEventId(), cached.get());
        }

        long start = System.currentTimeMillis();
        LoyaltyEventProcessRequest coreReq = toCoreRequest(request);

        LoyaltyEventProcessResponse core = campaignOrchestrationService.process(tenantId, coreReq);
        int processingTimeMs = (int) (System.currentTimeMillis() - start);

        if (core.isIdempotentReplay()) {
            Optional<EventProcessingResponse> fromCache = idempotencyService.getCachedResponse(tenantId, request.getEventId());
            if (fromCache.isPresent()) {
                return buildIdempotentReplay(request.getEventId(), fromCache.get());
            }
            EventProcessingResponse rebuilt = responseMapper.toSuccessResponse(request, core, null, processingTimeMs);
            idempotencyService.cacheResponse(tenantId, request.getEventId(), rebuilt);
            return buildIdempotentReplay(request.getEventId(), rebuilt);
        }

        if (!core.isSuccess()) {
            EventProcessingResponse failure = responseMapper.toSuccessResponse(request, core, null, processingTimeMs);
            failure.setStatus("ERROR");
            persistProcessingLog(
                tenantId, request, apiKeyUid, core, failure, requestPayloadHash, processingTimeMs, null
            );
            throw new IntegrationApiException(
                HttpStatus.BAD_REQUEST,
                "RULE_ERROR",
                core.getMessage() != null ? core.getMessage() : "Rule evaluation failed",
                false
            );
        }

        EventProcessingResponse response = responseMapper.toSuccessResponse(request, core, null, processingTimeMs);
        persistProcessingLog(
            tenantId, request, apiKeyUid, core, response, requestPayloadHash,
            processingTimeMs, null
        );
        idempotencyService.cacheResponse(tenantId, request.getEventId(), response);
        return response;
    }

    public ValidationResponse validateEvent(String tenantId, IntegrationEventRequest request) {
        Map<String, Object> payload = toValidationPayload(request);
        SandboxValidateEventRequest sandboxReq = new SandboxValidateEventRequest();
        try {
            sandboxReq.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event payload");
        }

        ValidationResponse response = new ValidationResponse();
        response.setEventId(request.getEventId());
        response.setTimestamp(Instant.now());
        response.setNote("Dry-run validation only — no rewards issued.");

        ValidationResponse.ValidationDetails validation = new ValidationResponse.ValidationDetails();
        validation.setSchemaValid(true);
        validation.setRequiredFieldsPresent(true);
        validation.setCustomFieldsValid(true);

        try {
            Map<String, Object> result = integrationService.validateSandboxEvent(tenantId, sandboxReq);
            response.setStatus("VALIDATION_SUCCESS");
            if (result.containsKey("ruleEvaluation")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleEvalMap = (Map<String, Object>) result.get("ruleEvaluation");
                ValidationResponse.DryRunResults dry = new ValidationResponse.DryRunResults();
                Object matched = ruleEvalMap.get("matchedRules");
                if (matched instanceof java.util.List<?> list) {
                    dry.setRulesMatched(list.size());
                }
                Object finalPts = ruleEvalMap.get("finalPointsAwarded");
                if (finalPts != null) {
                    dry.setPointsCalculated(new BigDecimal(String.valueOf(finalPts)));
                }
                response.setDryRunResults(dry);
            }
        } catch (ProgrammeConfigValidationException e) {
            validation.setSchemaValid(false);
            validation.getErrors().addAll(e.getFieldErrors().values());
            response.setValidation(validation);
            throw new IntegrationApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                e.getMessage(),
                false,
                e.getFieldErrors()
            );
        } catch (Exception e) {
            validation.setSchemaValid(false);
            validation.getErrors().add(e.getMessage());
            response.setValidation(validation);
            throw new IntegrationApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                e.getMessage() != null ? e.getMessage() : "Event validation failed",
                false,
                validation.getErrors()
            );
        }
        response.setValidation(validation);
        return response;
    }

    @Transactional(readOnly = true)
    public Optional<EventStatusResponse> getEventStatus(String tenantId, String eventId) {
        Optional<EventStatusResponse> fromLog = processingLogRepository.findByTenantIdAndEventId(tenantId, eventId)
            .map(log -> mapLogToStatus(eventId, log));
        if (fromLog.isPresent()) {
            return fromLog;
        }
        return idempotencyService.getCachedResponse(tenantId, eventId)
            .map(cached -> mapCachedToStatus(eventId, cached));
    }

    private static EventStatusResponse mapLogToStatus(String eventId, IntegrationEventProcessingLog log) {
        EventStatusResponse status = new EventStatusResponse();
        status.setEventId(eventId);
        status.setTimestamp(log.getCreatedAt());
        status.setProcessedAt(log.getUpdatedAt());
        status.setStatus(log.getProcessingStatus() == EventProcessingStatus.SUCCESS ? "SUCCESS" : "FAILURE");
        EventStatusResponse.ResultSummary result = new EventStatusResponse.ResultSummary();
        result.setPointsAwarded(log.getTotalPointsAwarded());
        result.setNewBalance(log.getNewBalance());
        result.setRulesMatched(log.getRulesMatchedCount());
        result.setCampaignsEligible(log.getCampaignsEligibleCount());
        status.setResult(result);
        return status;
    }

    private static EventStatusResponse mapCachedToStatus(String eventId, EventProcessingResponse cached) {
        EventStatusResponse status = new EventStatusResponse();
        status.setEventId(eventId);
        status.setTimestamp(cached.getTimestamp());
        status.setProcessedAt(cached.getTimestamp());
        status.setStatus("SUCCESS".equalsIgnoreCase(cached.getStatus()) ? "SUCCESS" : "FAILURE");
        EventStatusResponse.ResultSummary result = new EventStatusResponse.ResultSummary();
        if (cached.getEarnings() != null) {
            result.setPointsAwarded(cached.getEarnings().getFinalPoints());
            result.setNewBalance(cached.getEarnings().getNewBalance());
        }
        if (cached.getRules() != null && cached.getRules().getMatched() != null) {
            result.setRulesMatched(cached.getRules().getMatched().size());
        }
        if (cached.getCampaigns() != null && cached.getCampaigns().getEligible() != null) {
            result.setCampaignsEligible(cached.getCampaigns().getEligible().size());
        }
        status.setResult(result);
        return status;
    }

    private EventIdempotentReplayResponse buildIdempotentReplay(String eventId, EventProcessingResponse original) {
        EventIdempotentReplayResponse replay = new EventIdempotentReplayResponse();
        replay.setEventId(eventId);
        replay.setMessage("This event was already processed");
        replay.setOriginalResult(original);
        replay.setOriginalTimestamp(original.getTimestamp());
        replay.setRetryable(false);
        return replay;
    }

    private LoyaltyEventProcessRequest toCoreRequest(IntegrationEventRequest request) {
        Map<String, Object> payload = buildIntegrationPayloadFields(request);
        LoyaltyEventProcessRequest core = new LoyaltyEventProcessRequest();
        core.setProgrammeUid(stringField(payload, "programmeUid", "default"));
        core.setCustomerId(request.getCustomerId());
        core.setCustomerTierUid(request.getCustomerTierUid());
        core.setEventType(request.getEventType());
        core.setTransactionId(request.getEventId());
        core.setAmount(request.getAmount());
        Map<String, Object> metadata = extractMetadata(payload);
        if (!metadata.isEmpty()) {
            core.setMetadata(metadata);
        }
        ObjectNode eventPayload = objectMapper.createObjectNode();
        payload.forEach((key, value) -> eventPayload.set(key, objectMapper.valueToTree(value)));
        core.setEventPayload(eventPayload);
        return core;
    }

    private Map<String, Object> toValidationPayload(IntegrationEventRequest request) {
        return buildIntegrationPayloadFields(request);
    }

  /** Flat event map used for schema validation (validate + process paths). */
    private static Map<String, Object> buildIntegrationPayloadFields(IntegrationEventRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", request.getEventType());
        payload.put("transactionId", request.getEventId());
        payload.put("customerId", request.getCustomerId());
        payload.put("amount", request.getAmount());
        if (request.getProgrammeUid() != null && !request.getProgrammeUid().isBlank()) {
            payload.put("programmeUid", request.getProgrammeUid());
        }
        if (request.getTimestamp() != null) {
            payload.put("timestamp", request.getTimestamp().toString());
        }
        putIfPresent(payload, "currency", request.getCurrency());
        putIfPresent(payload, "channel", request.getChannel());
        putIfPresent(payload, "merchantId", request.getMerchantId());
        putIfPresent(payload, "transactionRef", request.getTransactionRef());
        if (request.getCustomerTierUid() != null) {
            payload.put("tierUid", request.getCustomerTierUid());
        }
        if (request.getMetadata() != null) {
            request.getMetadata().forEach(payload::putIfAbsent);
        }
        return payload;
    }

    private static Map<String, Object> extractMetadata(Map<String, Object> payload) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (!CORE_PAYLOAD_KEYS.contains(key)) {
                metadata.put(key, value);
            }
        });
        return metadata;
    }

    private static final java.util.Set<String> CORE_PAYLOAD_KEYS = java.util.Set.of(
        "programmeUid", "customerId", "eventType", "transactionId", "amount"
    );

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static String stringField(Map<String, Object> payload, String key, String defaultValue) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private void persistProcessingLog(
        String tenantId,
        IntegrationEventRequest request,
        String apiKeyUid,
        LoyaltyEventProcessResponse core,
        EventProcessingResponse response,
        String requestPayloadHash,
        int processingTimeMs,
        RuleEvaluationResponse ruleEval
    ) {
        IntegrationEventProcessingLog log = processingLogRepository
            .findByTenantIdAndEventId(tenantId, request.getEventId())
            .orElseGet(IntegrationEventProcessingLog::new);
        log.setTenantId(tenantId);
        log.setEventId(request.getEventId());
        log.setCustomerId(request.getCustomerId());
        log.setAmount(request.getAmount());
        log.setEventType(request.getEventType());
        log.setApiKeyUid(apiKeyUid);
        log.setProcessingStatus(core.isSuccess() ? EventProcessingStatus.SUCCESS : EventProcessingStatus.RULE_ERROR);
        log.setHttpStatus(core.isSuccess() ? 200 : 400);
        log.setRulesMatchedCount(ruleEval != null && ruleEval.getMatchedRules() != null
            ? ruleEval.getMatchedRules().size() : 0);
        log.setBasePointsCalculated(ruleEval != null ? ruleEval.getBasePointsCalculated() : BigDecimal.ZERO);
        log.setTierMultiplier(ruleEval != null && ruleEval.getTierMultiplier() != null
            ? ruleEval.getTierMultiplier() : BigDecimal.ONE);
        log.setTotalPointsAwarded(core.getTotalPointsAwarded());
        log.setNewBalance(core.getNewBalance());
        log.setCampaignsEligibleCount(core.getCampaignsApplied() != null ? core.getCampaignsApplied().size() : 0);
        log.setCampaignBonusPoints(core.getCampaignPointsAwarded());
        log.setProcessingTimeMs(processingTimeMs);
        log.setRequestPayloadHash(requestPayloadHash);
        try {
            log.setResponsePayloadJson(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ignored) {
            log.setResponsePayloadJson(null);
        }
        processingLogRepository.save(log);
    }

    public static String hashPayload(String body) {
        return IntegrationHmacVerifier.sha256Hex(body != null ? body : "");
    }
}
