package com.loyaltyos.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.config.IntegrationProperties;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IntegrationIdempotencyServiceTest {

  @Test
  void returnsCachedFromRedis() throws Exception {
    EventProcessingResponse response = new EventProcessingResponse();
    response.setEventId("evt_1");
    response.setStatus("SUCCESS");
    String json = new ObjectMapper().writeValueAsString(response);

    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get(contains("evt_1"))).thenReturn(json);

    IntegrationProperties props = new IntegrationProperties();
    IntegrationIdempotencyService svc = new IntegrationIdempotencyService(
        redis, new ObjectMapper(), props, mock(IntegrationEventProcessingLogRepository.class));

    Optional<EventProcessingResponse> cached = svc.getCachedResponse("t1", "evt_1");
    assertTrue(cached.isPresent());
    assertEquals("SUCCESS", cached.get().getStatus());
  }
}
