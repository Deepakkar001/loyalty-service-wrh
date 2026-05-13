package com.loyaltyos.onboarding.rules.service;

import com.loyaltyos.onboarding.rules.config.RulesProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Service
public class RuleEarningCapService {

    private static final Logger log = LoggerFactory.getLogger(RuleEarningCapService.class);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final StringRedisTemplate stringRedisTemplate;
    private final RulesProperties rulesProperties;

    public RuleEarningCapService(StringRedisTemplate stringRedisTemplate, RulesProperties rulesProperties) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate");
        this.rulesProperties = Objects.requireNonNull(rulesProperties, "rulesProperties");
    }

    /**
     * Returns the maximum points that may still be awarded today/month without exceeding programme caps.
     * When caps or Redis are disabled, returns {@code proposed} unchanged (caller uses full proposed).
     */
    public BigDecimal clipToCaps(
        String tenantId,
        String programmeUid,
        String customerId,
        BigDecimal proposed,
        BigDecimal dailyCap,
        BigDecimal monthlyCap,
        Instant now
    ) {
        if (!rulesProperties.isCapsRedisEnabled() || proposed == null || proposed.signum() <= 0) {
            return proposed;
        }
        try {
            String dk = dayKey(tenantId, programmeUid, customerId, now);
            String mk = monthKey(tenantId, programmeUid, customerId, now);

            BigDecimal usedDay = readDecimal(dk);
            BigDecimal usedMonth = readDecimal(mk);

            BigDecimal out = proposed;
            if (dailyCap != null && dailyCap.signum() > 0) {
                BigDecimal remain = dailyCap.subtract(usedDay).max(BigDecimal.ZERO);
                out = out.min(remain);
            }
            if (monthlyCap != null && monthlyCap.signum() > 0) {
                BigDecimal remain = monthlyCap.subtract(usedMonth).max(BigDecimal.ZERO);
                out = out.min(remain);
            }
            return out.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Redis cap read failed (fail-open, full proposed): {}", e.getMessage());
            return proposed;
        }
    }

    /**
     * Records earned points against day/month counters (called after clipping so totals match awards).
     */
    public void recordEarned(String tenantId, String programmeUid, String customerId, BigDecimal points, Instant now) {
        if (!rulesProperties.isCapsRedisEnabled() || points == null || points.signum() <= 0) {
            return;
        }
        try {
            ZonedDateTime utc = now.atZone(ZoneOffset.UTC);
            increment(dayKey(tenantId, programmeUid, customerId, now), points, ttlToEndOfUtcDay(utc));
            increment(monthKey(tenantId, programmeUid, customerId, now), points, ttlToEndOfUtcMonth(utc));
        } catch (Exception e) {
            log.warn("Redis cap increment failed: {}", e.getMessage());
        }
    }

    private BigDecimal readDecimal(String key) {
        String v = stringRedisTemplate.opsForValue().get(Objects.requireNonNull(key, "key"));
        if (v == null || v.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void increment(String key, BigDecimal delta, Duration ttl) {
        BigDecimal cur = readDecimal(key);
        BigDecimal next = cur.add(delta);
        stringRedisTemplate.opsForValue().set(
            Objects.requireNonNull(key, "key"),
            Objects.requireNonNull(next.toPlainString(), "value"),
            Objects.requireNonNull(ttl, "ttl")
        );
    }

    private static Duration ttlToEndOfUtcDay(ZonedDateTime utc) {
        ZonedDateTime end = utc.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC);
        return Duration.between(utc.toInstant(), end.toInstant()).plusSeconds(5);
    }

    private static Duration ttlToEndOfUtcMonth(ZonedDateTime utc) {
        ZonedDateTime end = utc.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC);
        return Duration.between(utc.toInstant(), end.toInstant()).plusSeconds(5);
    }

    private String dayKey(String tenantId, String programmeUid, String customerId, Instant now) {
        ZonedDateTime utc = now.atZone(ZoneOffset.UTC);
        return "earn:cap:day:" + tenantId + ":" + programmeUid + ":" + customerId + ":" + DAY.format(utc);
    }

    private String monthKey(String tenantId, String programmeUid, String customerId, Instant now) {
        ZonedDateTime utc = now.atZone(ZoneOffset.UTC);
        return "earn:cap:month:" + tenantId + ":" + programmeUid + ":" + customerId + ":" + MONTH.format(utc);
    }

    public BigDecimal dailyRemaining(String tenantId, String programmeUid, String customerId, BigDecimal dailyCap, Instant now) {
        if (dailyCap == null) {
            return null;
        }
        if (!rulesProperties.isCapsRedisEnabled()) {
            return dailyCap;
        }
        try {
            BigDecimal used = readDecimal(dayKey(tenantId, programmeUid, customerId, now));
            return dailyCap.subtract(used).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return dailyCap;
        }
    }

    public BigDecimal monthlyRemaining(String tenantId, String programmeUid, String customerId, BigDecimal monthlyCap, Instant now) {
        if (monthlyCap == null) {
            return null;
        }
        if (!rulesProperties.isCapsRedisEnabled()) {
            return monthlyCap;
        }
        try {
            BigDecimal used = readDecimal(monthKey(tenantId, programmeUid, customerId, now));
            return monthlyCap.subtract(used).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return monthlyCap;
        }
    }
}
