package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idem:register:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> type) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        String json = redis.opsForValue().get(PREFIX + idempotencyKey.trim());
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
            redis.opsForValue().set((PREFIX + idempotencyKey.trim()), json, TTL);
        } catch (JsonProcessingException ignored) {
            // Best effort.
        }
    }
}

