package com.loyaltyos.onboarding.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Programme-wide event schema settings (not tied to a single event type).
 */
public class EventSchemaSettingsPatchRequest {

    private Integer version;
    private Integer backwardCompatibilityDays;
    private JsonNode customFields;

    public EventSchemaSettingsPatchRequest() {}

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getBackwardCompatibilityDays() {
        return backwardCompatibilityDays;
    }

    public void setBackwardCompatibilityDays(Integer backwardCompatibilityDays) {
        this.backwardCompatibilityDays = backwardCompatibilityDays;
    }

    public JsonNode getCustomFields() {
        return customFields;
    }

    public void setCustomFields(JsonNode customFields) {
        this.customFields = customFields;
    }
}
