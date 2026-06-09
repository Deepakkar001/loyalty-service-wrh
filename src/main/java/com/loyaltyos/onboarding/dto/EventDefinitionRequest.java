package com.loyaltyos.onboarding.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Single event definition within programme or campaign {@code eventSchema.eventDefinitions[]}.
 */
public class EventDefinitionRequest {

    @NotNull
    private String eventType;

    @NotNull
    private JsonNode coreFields;

    public EventDefinitionRequest() {}

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public JsonNode getCoreFields() {
        return coreFields;
    }

    public void setCoreFields(JsonNode coreFields) {
        this.coreFields = coreFields;
    }
}
