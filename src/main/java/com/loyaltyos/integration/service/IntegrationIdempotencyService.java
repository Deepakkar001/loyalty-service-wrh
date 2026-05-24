package com.loyaltyos.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.config.IntegrationProperties;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.entity.IntegrationEventProcessingLog;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class IntegrationIdempotencyService {

    private static final String PREFIX = "integration:idem:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final IntegrationProperties properties;
    private final IntegrationEventProcessingLogRepository processingLogRepository;

    public IntegrationIdempotencyService(
        StringRedisTemplate redis,
        ObjectMapper objectMapper,
        IntegrationProperties properties,
        IntegrationEventProcessingLogRepository processingLogRepository
    ) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.processingLogRepository = Objects.requireNonNull(processingLogRepository, "processingLogRepository");
    }

    public Optional<EventProcessingResponse> getCachedResponse(String tenantId, String eventId) {
        String redisKey = cacheKey(tenantId, eventId);
        String json = redis.opsForValue().get(redisKey);
        if (json != null && !json.isBlank()) {
            return deserialize(json);
        }
        Instant since = Instant.now().minus(Duration.ofHours(properties.getIdempotency().getCacheTtlHours()));
        return processingLogRepository
            .findByTenantIdAndEventIdAndCreatedAtAfter(tenantId, eventId, since)
            .flatMap(this::fromLog);
    }

    public void cacheResponse(String tenantId, String eventId, EventProcessingResponse response) {
        if (response == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            Duration ttl = Duration.ofHours(properties.getIdempotency().getCacheTtlHours());
            redis.opsForValue().set(
                Objects.requireNonNull(cacheKey(tenantId, eventId), "key"),
                Objects.requireNonNull(json, "json"),
                Objects.requireNonNull(ttl, "ttl")
            );
        } catch (JsonProcessingException ignored) {
            // best effort
        }
    }

    private Optional<EventProcessingResponse> fromLog(IntegrationEventProcessingLog log) {
        if (log.getResponsePayloadJson() == null || log.getResponsePayloadJson().isBlank()) {
            return Optional.empty();
        }
        return deserialize(log.getResponsePayloadJson());
    }

    private Optional<EventProcessingResponse> deserialize(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, EventProcessingResponse.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private static String cacheKey(String tenantId, String eventId) {
        return PREFIX + tenantId + ":" + eventId;
    }
}
