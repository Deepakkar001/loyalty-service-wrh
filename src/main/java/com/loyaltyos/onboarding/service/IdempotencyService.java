package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idem:register:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> type) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        String json = redis.opsForValue().get(Objects.requireNonNull(PREFIX + idempotencyKey.trim(), "key"));
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            // Cache corruption should not break business flow.
            redis.delete(PREFIX + idempotencyKey.trim());
            return Optional.empty();
        }
    }

    @SuppressWarnings("null")
    public void cacheResponse(String idempotencyKey, Object response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null) return;
        try {
            String json = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(
                Objects.requireNonNull(PREFIX + idempotencyKey.trim(), "key"),
                Objects.requireNonNull(json, "json"),
                Objects.requireNonNull(TTL, "ttl")
            );
        } catch (JsonProcessingException ignored) {
            // Best effort.
        }
    }
}

