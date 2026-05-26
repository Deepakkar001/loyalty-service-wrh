package com.loyaltyos.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.exception.IntegrationApiException;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.service.ProgrammeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationEventPayloadResolverTest {

    @Mock
    private ProgrammeService programmeService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IntegrationEventPayloadResolver resolver;

    @Test
    void parseAndValidate_acceptsTenantSchemaFields() throws Exception {
        String configJson = """
            {
              "eventSchema": {
                "eventDefinitions": [{
                  "eventType": "OrderPlaced",
                  "coreFields": [
                    {"name": "transactionId", "type": "string", "required": true},
                    {"name": "Orderid", "type": "number", "required": true},
                    {"name": "Channel", "type": "string", "required": true},
                    {"name": "Timestamp", "type": "string", "required": true},
                    {"name": "CustomerId", "type": "number", "required": true}
                  ]
                }]
              }
            }
            """;
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson(configJson);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        var body = objectMapper.readTree("""
            {
              "eventType": "OrderPlaced",
              "eventId": "evt_postman_0019",
              "transactionId": "test_14665",
              "Timestamp": "2026-05-25T14:30:00Z",
              "CustomerId": 456789526,
              "Orderid": 124545454545,
              "Channel": "MOBILE_APP"
            }
            """);

        var parsed = resolver.parseAndValidate("t1", body);
        assertThat(parsed.eventType()).isEqualTo("OrderPlaced");
        assertThat(parsed.eventId()).isEqualTo("evt_postman_0019");
        assertThat(parsed.customerId()).isEqualTo("456789526");
        assertThat(parsed.schemaPayload()).containsKey("Orderid");
        assertThat(parsed.eventPayload().has("CustomerId")).isTrue();
    }

    @Test
    void buildRuleEvaluateRequest_resolvesSchemaFieldAliases() {
        var flat = new java.util.LinkedHashMap<String, Object>();
        flat.put("eventType", "OrderPlaced");
        flat.put("eventId", "evt_1");
        flat.put("CustomerId", "456789526");
        flat.put("Orderid", 124545454545L);
        flat.put("Channel", "MOBILE_APP");

        var req = resolver.buildRuleEvaluateRequest(flat);
        assertThat(req.getCustomerId()).isEqualTo("456789526");
        assertThat(req.getEventId()).isEqualTo("evt_1");
        assertThat(req.getEventType()).isEqualTo("OrderPlaced");
        assertThat(req.getChannel()).isEqualTo("MOBILE_APP");
        assertThat(req.getAmount()).isEqualByComparingTo("0");
        assertThat(req.getEventPayload().has("Channel")).isTrue();
    }

    @Test
    void parseAndValidate_legacyPurchaseStillWorks() throws Exception {
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(null);

        var body = objectMapper.readTree("""
            {
              "eventType": "PURCHASE",
              "eventId": "evt_1",
              "customerId": "cust_1",
              "amount": 500
            }
            """);

        var parsed = resolver.parseAndValidate("t1", body);
        assertThat(parsed.customerId()).isEqualTo("cust_1");
        assertThat(parsed.amount()).isEqualByComparingTo("500");
    }

    @Test
    void parseAndValidate_rejectsMissingSchemaFields() throws Exception {
        String configJson = """
            {
              "eventSchema": {
                "eventDefinitions": [{
                  "eventType": "OrderPlaced",
                  "coreFields": [
                    {"name": "CustomerId", "type": "number", "required": true}
                  ]
                }]
              }
            }
            """;
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson(configJson);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        var body = objectMapper.readTree("""
            {"eventType": "OrderPlaced", "eventId": "evt_1"}
            """);

        assertThatThrownBy(() -> resolver.parseAndValidate("t1", body))
            .isInstanceOf(IntegrationApiException.class)
            .satisfies(ex -> assertThat(((IntegrationApiException) ex).getErrorCode()).isEqualTo("VALIDATION_FAILED"));
    }
}
