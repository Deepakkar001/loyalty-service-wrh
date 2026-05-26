package com.loyaltyos.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.service.CampaignOrchestrationService;
import com.loyaltyos.integration.dto.EventIdempotentReplayResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.EventStatusResponse;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
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
        IntegrationParsedEvent parsed,
        String apiKeyUid,
        String requestBody,
        String requestPayloadHash
    ) {
        Optional<EventProcessingResponse> cached = idempotencyService.getCachedResponse(tenantId, parsed.eventId());
        if (cached.isPresent()) {
            return buildIdempotentReplay(parsed.eventId(), cached.get());
        }

        long start = System.currentTimeMillis();
        LoyaltyEventProcessRequest coreReq = toCoreRequest(parsed);

        LoyaltyEventProcessResponse core = campaignOrchestrationService.process(tenantId, coreReq);
        int processingTimeMs = (int) (System.currentTimeMillis() - start);

        if (core.isIdempotentReplay()) {
            Optional<EventProcessingResponse> fromCache = idempotencyService.getCachedResponse(tenantId, parsed.eventId());
            if (fromCache.isPresent()) {
                return buildIdempotentReplay(parsed.eventId(), fromCache.get());
            }
            EventProcessingResponse rebuilt = responseMapper.toSuccessResponse(parsed, core, null, processingTimeMs);
            idempotencyService.cacheResponse(tenantId, parsed.eventId(), rebuilt);
            return buildIdempotentReplay(parsed.eventId(), rebuilt);
        }

        if (!core.isSuccess()) {
            EventProcessingResponse failure = responseMapper.toSuccessResponse(parsed, core, null, processingTimeMs);
            failure.setStatus("ERROR");
            persistProcessingLog(
                tenantId, parsed, apiKeyUid, core, failure, requestPayloadHash, processingTimeMs, null
            );
            throw new IntegrationApiException(
                HttpStatus.BAD_REQUEST,
                "RULE_ERROR",
                core.getMessage() != null ? core.getMessage() : "Rule evaluation failed",
                false
            );
        }

        EventProcessingResponse response = responseMapper.toSuccessResponse(parsed, core, null, processingTimeMs);
        persistProcessingLog(
            tenantId, parsed, apiKeyUid, core, response, requestPayloadHash,
            processingTimeMs, null
        );
        idempotencyService.cacheResponse(tenantId, parsed.eventId(), response);
        return response;
    }

    public ValidationResponse validateEvent(String tenantId, IntegrationParsedEvent parsed) {
        SandboxValidateEventRequest sandboxReq = new SandboxValidateEventRequest();
        try {
            sandboxReq.setPayloadJson(objectMapper.writeValueAsString(parsed.schemaPayload()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event payload");
        }

        ValidationResponse response = new ValidationResponse();
        response.setEventId(parsed.eventId());
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

    private LoyaltyEventProcessRequest toCoreRequest(IntegrationParsedEvent parsed) {
        LoyaltyEventProcessRequest core = new LoyaltyEventProcessRequest();
        core.setProgrammeUid(parsed.programmeUid());
        core.setCustomerId(parsed.customerId());
        core.setEventType(parsed.eventType());
        core.setTransactionId(parsed.eventId());
        core.setAmount(parsed.amount());
        core.setCustomerTierUid(parsed.customerTierUid());
        core.setMetadata(parsed.metadata().isEmpty() ? null : new LinkedHashMap<>(parsed.metadata()));
        core.setEventPayload(parsed.eventPayload());
        return core;
    }

    private void persistProcessingLog(
        String tenantId,
        IntegrationParsedEvent parsed,
        String apiKeyUid,
        LoyaltyEventProcessResponse core,
        EventProcessingResponse response,
        String requestPayloadHash,
        int processingTimeMs,
        RuleEvaluationResponse ruleEval
    ) {
        IntegrationEventProcessingLog log = processingLogRepository
            .findByTenantIdAndEventId(tenantId, parsed.eventId())
            .orElseGet(IntegrationEventProcessingLog::new);
        log.setTenantId(tenantId);
        log.setEventId(parsed.eventId());
        log.setCustomerId(parsed.customerId());
        log.setAmount(parsed.amount());
        log.setEventType(parsed.eventType());
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
