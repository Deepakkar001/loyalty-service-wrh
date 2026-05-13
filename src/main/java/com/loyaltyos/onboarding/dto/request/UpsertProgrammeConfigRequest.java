package com.loyaltyos.onboarding.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public class UpsertProgrammeConfigRequest {
    @NotNull
    private JsonNode config;

    public UpsertProgrammeConfigRequest() {}

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }
}

