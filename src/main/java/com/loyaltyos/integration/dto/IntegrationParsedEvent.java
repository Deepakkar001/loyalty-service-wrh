package com.loyaltyos.integration.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Normalized integration event after JSON parse and programme {@code eventSchema} validation.
 * Preserves tenant-configured field names in {@link #schemaPayload()} and {@link #eventPayload()}.
 */
public record IntegrationParsedEvent(
    String eventType,
    String eventId,
    String programmeUid,
    String evaluationScope,
    String customerId,
    BigDecimal amount,
    String customerTierUid,
    Map<String, Object> schemaPayload,
    JsonNode eventPayload,
    Map<String, Object> metadata
) {}
