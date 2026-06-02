package com.loyaltyos.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import com.loyaltyos.integration.exception.IntegrationApiException;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.service.EventSchemaPayloadValidator;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rules.dto.RuleEvaluateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Parses integration event JSON using the tenant's active programme {@code eventSchema}.
 * Falls back to legacy required fields when no programme schema is configured.
 */
@Service
public class IntegrationEventPayloadResolver {

    private static final List<String> CUSTOMER_ID_KEYS = List.of(
        "customerId", "CustomerId", "customer_id", "CUSTOMER_ID"
    );
    private static final List<String> AMOUNT_KEYS = List.of("amount", "Amount", "orderAmount", "OrderAmount");
    private static final List<String> EVENT_ID_KEYS = List.of("eventId", "event_id");
    private static final List<String> TRANSACTION_ID_KEYS = List.of(
        "transactionId", "transaction_id", "Orderid", "orderId", "order_id"
    );
    private static final Set<String> METADATA_KEYS = Set.of(
        "channel", "Channel", "currency", "Currency", "merchantId", "merchant_id",
        "transactionRef", "transaction_ref", "timestamp", "Timestamp", "country", "Country",
        "tierUid", "customerTierUid"
    );

    private final ProgrammeService programmeService;
    private final ObjectMapper objectMapper;

    public IntegrationEventPayloadResolver(ProgrammeService programmeService, ObjectMapper objectMapper) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public IntegrationParsedEvent parseAndValidate(String tenantId, JsonNode body) {
        if (body == null || !body.isObject()) {
            throw validationFailed("Request body must be a JSON object", Map.of("body", "invalid"));
        }

        Map<String, Object> flat = objectMapper.convertValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
        flat = new LinkedHashMap<>(flat);

        String eventType = requiredScalar(flat, "eventType");
        String programmeUid = optionalScalar(flat, "programmeUid").orElse("default");
        flat.putIfAbsent("programmeUid", programmeUid);
        programmeService.assertProgrammeActiveForIntegration(tenantId, programmeUid);

        String evaluationScope = optionalScalar(flat, "evaluationScope").orElse(null);

        String eventId = resolveEventId(flat);
        flat.putIfAbsent("eventId", eventId);
        if (!flat.containsKey("transactionId") || isBlank(flat.get("transactionId"))) {
            flat.put("transactionId", firstPresentScalar(flat, TRANSACTION_ID_KEYS).orElse(eventId));
        }

        Map<String, String> schemaErrors = validateAgainstProgramme(tenantId, programmeUid, flat);
        if (!schemaErrors.isEmpty()) {
            throw validationFailed("Event payload validation failed", schemaErrors);
        }

        String customerId = firstPresentScalar(flat, CUSTOMER_ID_KEYS).orElse(null);
        if (customerId == null || customerId.isBlank()) {
            throw validationFailed(
                "Event payload validation failed",
                Map.of("customerId", "A customer identifier is required (e.g. customerId or CustomerId)")
            );
        }

        BigDecimal amount = resolveAmount(flat).orElse(BigDecimal.ZERO);
        String customerTierUid = firstPresentScalar(flat, List.of("customerTierUid", "tierUid", "TierUid")).orElse(null);

        ObjectNode eventPayload = body.deepCopy();
        Map<String, Object> metadata = extractMetadata(flat);

        return new IntegrationParsedEvent(
            eventType,
            eventId,
            programmeUid,
            evaluationScope,
            customerId,
            amount,
            customerTierUid,
            Map.copyOf(flat),
            eventPayload,
            metadata
        );
    }

    /**
     * Maps a validated flat event map to {@link RuleEvaluateRequest} (portal sandbox + integration).
     * Resolves customer / transaction / amount aliases used in programme event schemas.
     */
    public RuleEvaluateRequest buildRuleEvaluateRequest(Map<String, Object> flat) {
        Map<String, Object> copy = new LinkedHashMap<>(flat);
        String programmeUid = optionalScalar(copy, "programmeUid").orElse("default");
        String eventType = optionalScalar(copy, "eventType")
            .orElseThrow(() -> new IllegalArgumentException("eventType is required for rule evaluation"));
        String customerId = firstPresentScalar(copy, CUSTOMER_ID_KEYS)
            .orElseThrow(() -> new IllegalArgumentException(
                "A customer identifier is required (e.g. customerId or CustomerId)"));
        String eventId = firstPresentScalar(copy, EVENT_ID_KEYS)
            .or(() -> firstPresentScalar(copy, TRANSACTION_ID_KEYS))
            .orElse("sandbox_" + UUID.randomUUID());
        BigDecimal amount = resolveAmount(copy).orElse(BigDecimal.ZERO);
        String channel = firstPresentScalar(copy, List.of("channel", "Channel")).orElse(null);
        String merchantId = firstPresentScalar(copy, List.of("merchantId", "merchant_id")).orElse(null);
        String tierUid = firstPresentScalar(copy, List.of("customerTierUid", "tierUid", "TierUid")).orElse(null);

        return RuleEvaluateRequest.builder()
            .programmeUid(programmeUid)
            .customerId(customerId)
            .customerTierUid(tierUid)
            .eventId(eventId)
            .eventType(eventType)
            .amount(amount)
            .eventPayload(objectMapper.valueToTree(copy))
            .channel(channel)
            .merchantId(merchantId)
            .build();
    }

    private Map<String, String> validateAgainstProgramme(
        String tenantId,
        String programmeUid,
        Map<String, Object> payload
    ) {
        ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
        if (cfg != null && cfg.getConfigJson() != null && !cfg.getConfigJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(cfg.getConfigJson());
                return EventSchemaPayloadValidator.validatePayload(payload, root);
            } catch (JsonProcessingException e) {
                return Map.of("programmeConfig", "Unable to read programme configuration");
            }
        }
        return validateLegacyPayload(payload);
    }

    private static Map<String, String> validateLegacyPayload(Map<String, Object> payload) {
        Map<String, String> errors = new LinkedHashMap<>();
        requireField(payload, "eventType", errors);
        requireField(payload, "transactionId", errors);
        if (!payload.containsKey("transactionId") || isBlank(payload.get("transactionId"))) {
            requireField(payload, "eventId", errors);
        }
        boolean hasCustomer = CUSTOMER_ID_KEYS.stream().anyMatch(k -> !isBlank(payload.get(k)));
        if (!hasCustomer) {
            errors.put("customerId", "customerId is required");
        }
        boolean hasAmount = AMOUNT_KEYS.stream().anyMatch(k -> payload.get(k) != null);
        if (!hasAmount) {
            errors.put("amount", "amount is required");
        }
        return errors;
    }

    private static String resolveEventId(Map<String, Object> flat) {
        return firstPresentScalar(flat, EVENT_ID_KEYS)
            .or(() -> firstPresentScalar(flat, TRANSACTION_ID_KEYS))
            .orElseThrow(() -> validationFailed(
                "Event payload validation failed",
                Map.of("eventId", "eventId or transactionId is required")
            ));
    }

    private static java.util.Optional<BigDecimal> resolveAmount(Map<String, Object> flat) {
        for (String key : AMOUNT_KEYS) {
            if (!flat.containsKey(key) || flat.get(key) == null) {
                continue;
            }
            try {
                return java.util.Optional.of(new BigDecimal(String.valueOf(flat.get(key)).trim()));
            } catch (NumberFormatException ignored) {
                // try next key
            }
        }
        return java.util.Optional.empty();
    }

    private static Map<String, Object> extractMetadata(Map<String, Object> flat) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : flat.entrySet()) {
            String key = e.getKey();
            if (METADATA_KEYS.contains(key)) {
                metadata.put(normalizeMetadataKey(key), e.getValue());
            }
        }
        return metadata;
    }

    private static String normalizeMetadataKey(String key) {
        return switch (key) {
            case "Channel" -> "channel";
            case "Currency" -> "currency";
            case "Timestamp" -> "timestamp";
            case "Country" -> "country";
            case "TierUid" -> "tierUid";
            default -> key;
        };
    }

    private static String requiredScalar(Map<String, Object> flat, String key) {
        String v = optionalScalar(flat, key).orElse(null);
        if (v == null || v.isBlank()) {
            throw validationFailed(
                "Event payload validation failed",
                Map.of(key, key + " is required")
            );
        }
        return v;
    }

    private static java.util.Optional<String> optionalScalar(Map<String, Object> flat, String key) {
        if (!flat.containsKey(key) || flat.get(key) == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(String.valueOf(flat.get(key)).trim());
    }

    private static java.util.Optional<String> firstPresentScalar(Map<String, Object> flat, List<String> keys) {
        for (String key : keys) {
            if (flat.containsKey(key) && !isBlank(flat.get(key))) {
                return java.util.Optional.of(String.valueOf(flat.get(key)).trim());
            }
        }
        return java.util.Optional.empty();
    }

    private static void requireField(Map<String, Object> payload, String key, Map<String, String> errors) {
        if (!payload.containsKey(key) || isBlank(payload.get(key))) {
            errors.put(key, key + " is required");
        }
    }

    private static boolean isBlank(Object v) {
        return v == null || String.valueOf(v).isBlank();
    }

    private static IntegrationApiException validationFailed(String message, Map<String, String> details) {
        return new IntegrationApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, false, details);
    }
}
