package com.loyaltyos.access.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
public class TenantInviteTokenService {

    private static final String PREFIX = "tenant-invite:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public TenantInviteTokenService(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }

    public String issueToken(String tenantId, String userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String key = PREFIX + token;
        redis.opsForValue().set(key, tenantId + ":" + userId, TTL);
        return token;
    }

    public Optional<InviteTarget> consumeToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String key = PREFIX + token.trim();
        String value = redis.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        redis.delete(key);
        int sep = value.indexOf(':');
        if (sep <= 0 || sep >= value.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(new InviteTarget(value.substring(0, sep), value.substring(sep + 1)));
    }

    public record InviteTarget(String tenantId, String userId) {}
}
