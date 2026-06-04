package com.loyaltyos.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.service.CampaignOrchestrationService;
import com.loyaltyos.integration.dto.EventIdempotentReplayResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import com.loyaltyos.integration.exception.IntegrationApiException;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import com.loyaltyos.onboarding.service.IntegrationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
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

    IntegrationParsedEvent parsed = sampleParsed("evt_1", "c1", "PURCHASE");

    Object result = svc.processEvent("t1", parsed, "key1", "{}", "hash");
    assertInstanceOf(EventIdempotentReplayResponse.class, result);
  }

  @Test
  void processEvent_passesFullEventPayloadToOrchestration() {
    CampaignOrchestrationService orchestration = mock(CampaignOrchestrationService.class);
    LoyaltyEventProcessResponse success = new LoyaltyEventProcessResponse();
    success.setSuccess(true);
    ArgumentCaptor<LoyaltyEventProcessRequest> requestCaptor = ArgumentCaptor.forClass(LoyaltyEventProcessRequest.class);
    when(orchestration.process(eq("t1"), requestCaptor.capture())).thenReturn(success);

    IntegrationResponseMapper mapper = mock(IntegrationResponseMapper.class);
    EventProcessingResponse mapped = new EventProcessingResponse();
    when(mapper.toSuccessResponse(any(), eq(success), isNull(), anyInt())).thenReturn(mapped);

    ObjectMapper om = new ObjectMapper();
    var eventPayload = om.createObjectNode();
    eventPayload.put("Orderid", 999);
    eventPayload.put("Channel", "MOBILE_APP");

    IntegrationParsedEvent parsed = new IntegrationParsedEvent(
        "OrderPlaced",
        "evt_3",
        "default",
        null,
        null,
        "456",
        BigDecimal.ZERO,
        null,
        Map.of("Orderid", 999),
        eventPayload,
        Map.of("channel", "MOBILE_APP")
    );

    IntegrationEventService svc = new IntegrationEventService(
        orchestration,
        mock(IntegrationService.class),
        mock(IntegrationIdempotencyService.class),
        mapper,
        mock(IntegrationEventProcessingLogRepository.class),
        om
    );

    svc.processEvent("t1", parsed, "key1", "{}", "hash");

    LoyaltyEventProcessRequest core = requestCaptor.getValue();
    assertEquals("456", core.getCustomerId());
    assertNull(core.getCampaignUid());
    assertTrue(core.getEventPayload().has("Orderid"));
    assertEquals("MOBILE_APP", core.getMetadata().get("channel"));
  }

  @Test
  void processEvent_mapsCampaignUidToCoreRequest() {
    CampaignOrchestrationService orchestration = mock(CampaignOrchestrationService.class);
    LoyaltyEventProcessResponse success = new LoyaltyEventProcessResponse();
    success.setSuccess(true);
    when(orchestration.process(eq("t1"), any(LoyaltyEventProcessRequest.class))).thenReturn(success);

    IntegrationResponseMapper mapper = mock(IntegrationResponseMapper.class);
    EventProcessingResponse mapped = new EventProcessingResponse();
    when(mapper.toSuccessResponse(any(), eq(success), isNull(), anyInt())).thenReturn(mapped);

    ArgumentCaptor<LoyaltyEventProcessRequest> requestCaptor = ArgumentCaptor.forClass(LoyaltyEventProcessRequest.class);

    IntegrationEventService svc = new IntegrationEventService(
        orchestration,
        mock(IntegrationService.class),
        mock(IntegrationIdempotencyService.class),
        mapper,
        mock(IntegrationEventProcessingLogRepository.class),
        new ObjectMapper()
    );

    IntegrationParsedEvent parsed = new IntegrationParsedEvent(
        "PURCHASE",
        "evt_camp",
        "default",
        "CAMPAIGN",
        "a05c4589-ce7d-4686-96a4-9bc22b313b86",
        "dev_target_002",
        BigDecimal.valueOf(500),
        null,
        Map.of(),
        new ObjectMapper().createObjectNode(),
        Map.of("channel", "WEB")
    );

    svc.processEvent("t1", parsed, "key1", "{}", "hash");

    verify(orchestration).process(eq("t1"), requestCaptor.capture());
    assertEquals("a05c4589-ce7d-4686-96a4-9bc22b313b86", requestCaptor.getValue().getCampaignUid());
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

    assertThrows(IntegrationApiException.class,
        () -> svc.processEvent("t1", sampleParsed("evt_2", "c1", "PURCHASE"), "key1", "{}", "hash"));
  }

  private static IntegrationParsedEvent sampleParsed(String eventId, String customerId, String eventType) {
    return new IntegrationParsedEvent(
        eventType,
        eventId,
        "default",
        null,
        null,
        customerId,
        BigDecimal.TEN,
        null,
        Map.of("customerId", customerId, "eventType", eventType, "eventId", eventId, "amount", BigDecimal.TEN),
        new ObjectMapper().createObjectNode(),
        Map.of()
    );
  }
}
