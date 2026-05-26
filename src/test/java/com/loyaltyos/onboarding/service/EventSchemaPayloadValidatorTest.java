package com.loyaltyos.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventSchemaPayloadValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validatePayload_rejectsStringForNumberField() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "PURCHASE",
              "coreFields": [
                {"name": "amount", "type": "number", "required": true},
                {"name": "customerId", "type": "string", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "PURCHASE",
            "amount", "500",
            "customerId", "c1"
        );

        var errors = EventSchemaPayloadValidator.validatePayload(payload, root);
        assertThat(errors).containsEntry("amount", "amount must be a number");
    }

    @Test
    void validatePayload_acceptsNumericAmount() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "PURCHASE",
              "coreFields": [
                {"name": "amount", "type": "number", "required": true},
                {"name": "customerId", "type": "string", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "PURCHASE");
        payload.put("amount", 500);
        payload.put("customerId", "c1");

        assertThat(EventSchemaPayloadValidator.validatePayload(payload, root)).isEmpty();
    }

    @Test
    void validatePayload_rejectsNonIntegerForIntegerField() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "OrderPlaced",
              "coreFields": [
                {"name": "Orderid", "type": "integer", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "OrderPlaced",
            "Orderid", 124.5
        );

        var errors = EventSchemaPayloadValidator.validatePayload(payload, root);
        assertThat(errors).containsEntry("Orderid", "Orderid must be an integer");
    }

    @Test
    void validatePayload_acceptsIntegerField() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "OrderPlaced",
              "coreFields": [
                {"name": "Orderid", "type": "integer", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "OrderPlaced",
            "Orderid", 124545454545L
        );

        assertThat(EventSchemaPayloadValidator.validatePayload(payload, root)).isEmpty();
    }

    @Test
    void validatePayload_rejectsInvalidDateTime() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "SignupBonus",
              "coreFields": [
                {"name": "Timestamp", "type": "date-time", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "SignupBonus",
            "Timestamp", "not-a-date"
        );

        var errors = EventSchemaPayloadValidator.validatePayload(payload, root);
        assertThat(errors).containsEntry("Timestamp", "Timestamp must be a date-time string (ISO-8601)");
    }

    @Test
    void validatePayload_acceptsIsoDateTime() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "SignupBonus",
              "coreFields": [
                {"name": "Timestamp", "type": "date-time", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "SignupBonus",
            "Timestamp", "2026-05-25T13:44:05.029Z"
        );

        assertThat(EventSchemaPayloadValidator.validatePayload(payload, root)).isEmpty();
    }

    @Test
    void validatePayload_rejectsNumberForStringField() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "SignupBonus",
              "coreFields": [
                {"name": "CustomerId", "type": "string", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "SignupBonus",
            "CustomerId", 12345
        );

        var errors = EventSchemaPayloadValidator.validatePayload(payload, root);
        assertThat(errors).containsEntry("CustomerId", "CustomerId must be a string");
    }

    @Test
    void validatePayload_rejectsNumericStringForNumberField() throws Exception {
        var root = programmeWithDefinition(
            """
            {
              "eventType": "OrderPlaced",
              "coreFields": [
                {"name": "CustomerId", "type": "number", "required": true}
              ]
            }
            """
        );
        Map<String, Object> payload = Map.of(
            "eventType", "OrderPlaced",
            "CustomerId", "456789526"
        );

        var errors = EventSchemaPayloadValidator.validatePayload(payload, root);
        assertThat(errors).containsEntry("CustomerId", "CustomerId must be a number");
    }

    private com.fasterxml.jackson.databind.JsonNode programmeWithDefinition(String definitionJson) throws Exception {
        String json =
            """
            {
              "eventSchema": {
                "eventDefinitions": [%s],
                "customFields": []
              }
            }
            """.formatted(definitionJson.trim());
        return objectMapper.readTree(json);
    }
}
