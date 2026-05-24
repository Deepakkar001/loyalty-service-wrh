package com.loyaltyos.integration.service;

import com.loyaltyos.integration.config.IntegrationProperties;
import com.loyaltyos.integration.repository.ApiRequestAuditLogRepository;
import com.loyaltyos.onboarding.entity.TenantApiKey;
import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IntegrationRateLimitServiceTest {

  @Test
  void redisEnabled_blocksWhenOverLimit() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString())).thenReturn(1001L);

    IntegrationProperties props = new IntegrationProperties();
    props.getRateLimit().setRedisEnabled(true);
    props.getRateLimit().setDefaultRequestsPerHour(1000);

    IntegrationRateLimitService svc = new IntegrationRateLimitService(
        redis, props, mock(ApiRequestAuditLogRepository.class));

    TenantApiKey key = TenantApiKey.builder()
        .keyUid("k1")
        .tenantId("t1")
        .keyPrefix("los_sandbox_abcd")
        .keyHash("hash")
        .signingSecretHash("sh")
        .environment(ApiKeyEnvironment.SANDBOX)
        .status(ApiKeyStatus.ACTIVE)
        .build();

    IntegrationRateLimitService.RateLimitResult result = svc.check(key);
    assertFalse(result.allowed());
  }

  @Test
  void mysqlFallback_usesAuditCount() {
    ApiRequestAuditLogRepository auditRepo = mock(ApiRequestAuditLogRepository.class);
    when(auditRepo.countByApiKeyUidSince(eq("k1"), eq("t1"), any(Instant.class))).thenReturn(5L);

    IntegrationProperties props = new IntegrationProperties();
    props.getRateLimit().setRedisEnabled(false);

    IntegrationRateLimitService svc = new IntegrationRateLimitService(
        mock(StringRedisTemplate.class), props, auditRepo);

    TenantApiKey key = TenantApiKey.builder()
        .keyUid("k1")
        .tenantId("t1")
        .keyPrefix("p")
        .keyHash("h")
        .signingSecretHash("s")
        .environment(ApiKeyEnvironment.SANDBOX)
        .status(ApiKeyStatus.ACTIVE)
        .rateLimitRequests(1000)
        .build();

    assertTrue(svc.check(key).allowed());
  }
}
