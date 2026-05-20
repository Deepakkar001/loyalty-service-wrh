package com.loyaltyos.campaigns.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Campaign-scoped event schema (same JSON shape as programme {@code eventSchema}).
 */
public class CampaignEventSchemaUpsertRequest {

    @NotNull
    private JsonNode eventSchema;

    public CampaignEventSchemaUpsertRequest() {}

    public JsonNode getEventSchema() {
        return eventSchema;
    }

    public void setEventSchema(JsonNode eventSchema) {
        this.eventSchema = eventSchema;
    }
}
