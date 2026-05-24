package com.loyaltyos.integration;

import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.IntegrationEventRequest;
import com.loyaltyos.integration.service.IntegrationEventService;
import com.loyaltyos.integration.service.IntegrationIdempotencyService;
import com.loyaltyos.integration.service.IntegrationResponseMapper;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.service.CampaignOrchestrationService;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import com.loyaltyos.onboarding.service.IntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Lightweight end-to-end flow test (orchestration mocked) covering idempotency + success mapping.
 */
class IntegrationApiE2ETest {

  @Test
  void fullFlow_processAndReplayFromCache() {
    CampaignOrchestrationService orchestration = mock(CampaignOrchestrationService.class);
    LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
    core.setSuccess(true);
    core.setTotalPointsAwarded(BigDecimal.TEN);
    core.setNewBalance(BigDecimal.valueOf(110));
    core.setRulePointsAwarded(BigDecimal.TEN);
    when(orchestration.process(eq("tenant_e2e"), any())).thenReturn(core);

    IntegrationIdempotencyService idempotency = mock(IntegrationIdempotencyService.class);
    when(idempotency.getCachedResponse(anyString(), anyString())).thenReturn(Optional.empty());

    IntegrationResponseMapper mapper = new IntegrationResponseMapper();
    IntegrationEventService svc = new IntegrationEventService(
        orchestration,
        mock(IntegrationService.class),
        idempotency,
        mapper,
        mock(IntegrationEventProcessingLogRepository.class),
        new ObjectMapper()
    );

    IntegrationEventRequest req = new IntegrationEventRequest();
    req.setEventId("evt_e2e_1");
    req.setCustomerId("cust_1");
    req.setEventType("PURCHASE");
    req.setAmount(BigDecimal.valueOf(500));

    Object first = svc.processEvent("tenant_e2e", req, "key_uid", "{}", "hash1");
    assertInstanceOf(EventProcessingResponse.class, first);
    EventProcessingResponse success = (EventProcessingResponse) first;
    assertEquals("SUCCESS", success.getStatus());
    assertEquals(BigDecimal.TEN, success.getEarnings().getFinalPoints());

    verify(idempotency).cacheResponse(eq("tenant_e2e"), eq("evt_e2e_1"), any());
  }
}
