package com.loyaltyos.integration.dto;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import jakarta.validation.constraints.NotNull;

public class GenerateCredentialRequest {

    @NotNull
    private ApiKeyEnvironment environment = ApiKeyEnvironment.SANDBOX;

    private String name;
    private String description;

    public ApiKeyEnvironment getEnvironment() { return environment; }
    public void setEnvironment(ApiKeyEnvironment environment) { this.environment = environment; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
