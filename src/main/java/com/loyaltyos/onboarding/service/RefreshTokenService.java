package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "rt:";
    private static final SecureRandom RNG = new SecureRandom();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;

    public record RefreshPrincipal(String tenantId, String email, String role) {}

    public String issue(RefreshPrincipal principal) {
        String raw = generateToken();
        String hash = sha256Hex(raw);
        String key = PREFIX + hash;
        Duration ttl = Duration.ofDays(jwtProperties.getRefreshTtlDays());
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(principal), ttl);
        } catch (JsonProcessingException e) {
            // Best-effort: if JSON fails, do not issue a refresh token.
            throw new IllegalStateException("Failed to issue refresh token", e);
        }
        return raw;
    }

    public Optional<RefreshPrincipal> consumeAndRotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        String hash = sha256Hex(rawToken);
        String key = PREFIX + hash;
        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) return Optional.empty();

        // Rotation: delete old token immediately, caller must set a new cookie.
        redis.delete(key);
        try {
            return Optional.of(objectMapper.readValue(json, RefreshPrincipal.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        redis.delete(PREFIX + sha256Hex(rawToken));
    }

    private static String generateToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

