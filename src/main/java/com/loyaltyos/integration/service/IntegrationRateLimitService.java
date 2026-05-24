package com.loyaltyos.integration.service;

import com.loyaltyos.integration.config.IntegrationProperties;
import com.loyaltyos.integration.repository.ApiRequestAuditLogRepository;
import com.loyaltyos.onboarding.entity.TenantApiKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class IntegrationRateLimitService {

    private static final String PREFIX = "integration:rl:";
    private static final DateTimeFormatter HOUR_FMT =
        DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;
    private final IntegrationProperties properties;
    private final ApiRequestAuditLogRepository auditLogRepository;

    public IntegrationRateLimitService(
        StringRedisTemplate redis,
        IntegrationProperties properties,
        ApiRequestAuditLogRepository auditLogRepository
    ) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
    }

    public RateLimitResult check(TenantApiKey apiKey) {
        int limit = apiKey.getRateLimitRequests() != null
            ? apiKey.getRateLimitRequests()
            : properties.getRateLimit().getDefaultRequestsPerHour();

        long used;
        if (properties.getRateLimit().isRedisEnabled()) {
            used = incrementRedis(apiKey.getKeyUid());
        } else {
            Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
            used = auditLogRepository.countByApiKeyUidSince(apiKey.getKeyUid(), apiKey.getTenantId(), since);
        }

        int remaining = (int) Math.max(0, limit - used);
        if (used > limit) {
            return RateLimitResult.exceeded(limit, 0, resetAtEndOfHour());
        }
        return RateLimitResult.allowed(limit, remaining);
    }

    private long incrementRedis(String keyUid) {
        String hour = HOUR_FMT.format(Instant.now());
        String key = PREFIX + keyUid + ":" + hour;
        Long count = redis.opsForValue().increment(Objects.requireNonNull(key, "key"));
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofHours(1));
        }
        return count != null ? count : 0L;
    }

    private static Instant resetAtEndOfHour() {
        Instant now = Instant.now();
        return now.plusSeconds(3600 - (now.getEpochSecond() % 3600));
    }

    public record RateLimitResult(boolean allowed, int limit, int remaining, Instant resetAt) {
        public static RateLimitResult allowed(int limit, int remaining) {
            return new RateLimitResult(true, limit, remaining, resetAtEndOfHour());
        }

        public static RateLimitResult exceeded(int limit, int remaining, Instant resetAt) {
            return new RateLimitResult(false, limit, remaining, resetAt);
        }
    }
}
