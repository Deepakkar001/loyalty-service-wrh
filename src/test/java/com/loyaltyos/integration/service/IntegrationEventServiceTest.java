package com.loyaltyos.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.service.CampaignOrchestrationService;
import com.loyaltyos.integration.dto.EventIdempotentReplayResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.IntegrationEventRequest;
import com.loyaltyos.integration.exception.IntegrationApiException;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import com.loyaltyos.onboarding.service.IntegrationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class IntegrationEventServiceTest {

  @Test
  void processEvent_returnsIdempotentReplayWhenCached() {
    EventProcessingResponse cached = new EventProcessingResponse();
    cached.setEventId("evt_1");
    cached.setStatus("SUCCESS");

    IntegrationIdempotencyService idempotency = mock(IntegrationIdempotencyService.class);
    when(idempotency.getCachedResponse("t1", "evt_1")).thenReturn(Optional.of(cached));

    IntegrationEventService svc = new IntegrationEventService(
        mock(CampaignOrchestrationService.class),
        mock(IntegrationService.class),
        idempotency,
        mock(IntegrationResponseMapper.class),
        mock(IntegrationEventProcessingLogRepository.class),
        new ObjectMapper()
    );

    IntegrationEventRequest req = new IntegrationEventRequest();
    req.setEventId("evt_1");
    req.setCustomerId("c1");
    req.setEventType("PURCHASE");
    req.setAmount(BigDecimal.TEN);

    Object result = svc.processEvent("t1", req, "key1", "{}", "hash");
    assertInstanceOf(EventIdempotentReplayResponse.class, result);
  }

  @Test
  void processEvent_mapsTimestampAndChannelIntoOrchestrationPayload() {
    CampaignOrchestrationService orchestration = mock(CampaignOrchestrationService.class);
    LoyaltyEventProcessResponse success = new LoyaltyEventProcessResponse();
    success.setSuccess(true);
  ArgumentCaptor<LoyaltyEventProcessRequest> requestCaptor = ArgumentCaptor.forClass(LoyaltyEventProcessRequest.class);
    when(orchestration.process(eq("t1"), requestCaptor.capture())).thenReturn(success);

    IntegrationResponseMapper mapper = mock(IntegrationResponseMapper.class);
    EventProcessingResponse mapped = new EventProcessingResponse();
    when(mapper.toSuccessResponse(any(), eq(success), isNull(), anyInt())).thenReturn(mapped);

    IntegrationEventService svc = new IntegrationEventService(
        orchestration,
        mock(IntegrationService.class),
        mock(IntegrationIdempotencyService.class),
        mapper,
        mock(IntegrationEventProcessingLogRepository.class),
        new ObjectMapper()
    );

    IntegrationEventRequest req = new IntegrationEventRequest();
    req.setEventId("evt_3");
    req.setCustomerId("c1");
    req.setEventType("PURCHASE");
    req.setAmount(BigDecimal.valueOf(500));
    req.setTimestamp(Instant.parse("2026-05-24T14:30:00Z"));
    req.setChannel("MOBILE_APP");
    req.setCurrency("INR");

    svc.processEvent("t1", req, "key1", "{}", "hash");

    LoyaltyEventProcessRequest core = requestCaptor.getValue();
    assertNotNull(core.getMetadata());
    assertEquals("2026-05-24T14:30:00Z", core.getMetadata().get("timestamp"));
    assertEquals("MOBILE_APP", core.getMetadata().get("channel"));
    assertNotNull(core.getEventPayload());
    assertTrue(core.getEventPayload().has("timestamp"));
  }

  @Test
  void processEvent_throwsWhenOrchestrationFails() {
    CampaignOrchestrationService orchestration = mock(CampaignOrchestrationService.class);
    LoyaltyEventProcessResponse failure = new LoyaltyEventProcessResponse();
    failure.setSuccess(false);
    failure.setMessage("rule failed");
    when(orchestration.process(eq("t1"), any(LoyaltyEventProcessRequest.class))).thenReturn(failure);

    IntegrationResponseMapper mapper = mock(IntegrationResponseMapper.class);
    EventProcessingResponse mapped = new EventProcessingResponse();
    when(mapper.toSuccessResponse(any(), eq(failure), isNull(), anyInt())).thenReturn(mapped);

    IntegrationEventService svc = new IntegrationEventService(
        orchestration,
        mock(IntegrationService.class),
        mock(IntegrationIdempotencyService.class),
        mapper,
        mock(IntegrationEventProcessingLogRepository.class),
        new ObjectMapper()
    );

    IntegrationEventRequest req = new IntegrationEventRequest();
    req.setEventId("evt_2");
    req.setCustomerId("c1");
    req.setEventType("PURCHASE");
    req.setAmount(BigDecimal.TEN);

    assertThrows(IntegrationApiException.class,
        () -> svc.processEvent("t1", req, "key1", "{}", "hash"));
  }
}
