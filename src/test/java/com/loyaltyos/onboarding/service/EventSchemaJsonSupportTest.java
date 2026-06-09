package com.loyaltyos.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.onboarding.dto.EventDefinitionRequest;
import org.junit.jupiter.api.Test;

class EventSchemaJsonSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractEventFieldAllowlistForTrigger_usesMatchingDefinitionOnly() throws Exception {
        var root = objectMapper.readTree(
            """
            {
              "eventDefinitions": [
                {
                  "eventType": "PURCHASE",
                  "coreFields": [
                    {"name": "amount", "type": "number", "required": true}
                  ]
                },
                {
                  "eventType": "LOGIN",
                  "coreFields": [
                    {"name": "sessionId", "type": "string", "required": true}
                  ]
                }
              ],
              "customFields": []
            }
            """
        );

        var purchase = EventSchemaJsonSupport.extractEventFieldAllowlistForTrigger(root, "PURCHASE");
        assertThat(purchase).containsExactly("amount");

        var login = EventSchemaJsonSupport.extractEventFieldAllowlistForTrigger(root, "LOGIN");
        assertThat(login).containsExactly("sessionId");
    }

    @Test
    void triggerTypesFromEventSchema_buildsCommaSeparatedList() throws Exception {
        var root = objectMapper.readTree(
            """
            {
              "eventDefinitions": [
                {"eventType": "PURCHASE", "coreFields": [{"name": "amount", "type": "number", "required": true}]},
                {"eventType": "LOGIN", "coreFields": [{"name": "sessionId", "type": "string", "required": true}]}
              ]
            }
            """
        );

        assertThat(EventSchemaJsonSupport.triggerTypesFromEventSchema(root)).isEqualTo("PURCHASE,LOGIN");
    }

    @Test
    void replaceEventDefinition_updatesOnlyMatchingEvent() throws Exception {
        ObjectNode schema = (ObjectNode) objectMapper.readTree(
            """
            {
              "version": 2,
              "eventDefinitions": [
                {"eventType": "PURCHASE", "coreFields": [{"name": "amount", "type": "number", "required": true}]},
                {"eventType": "LOGIN", "coreFields": [{"name": "sessionId", "type": "string", "required": true}]}
              ],
              "customFields": []
            }
            """
        );

        EventDefinitionRequest patch = new EventDefinitionRequest();
        patch.setEventType("PURCHASE");
        patch.setCoreFields(objectMapper.readTree(
            "[{\"name\":\"amount\",\"type\":\"number\",\"required\":true},{\"name\":\"channel\",\"type\":\"string\",\"required\":false}]"
        ));

        EventSchemaJsonSupport.replaceEventDefinition(
            schema,
            "purchase",
            EventSchemaJsonSupport.toEventDefinitionNode(patch)
        );

        assertThat(schema.path("eventDefinitions").get(0).path("coreFields")).hasSize(2);
        assertThat(schema.path("eventDefinitions").get(1).path("coreFields")).hasSize(1);
        assertThat(schema.path("standardFields")).isNotEmpty();
    }
}
