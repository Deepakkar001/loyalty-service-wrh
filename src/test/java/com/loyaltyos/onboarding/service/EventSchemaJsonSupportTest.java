package com.loyaltyos.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
