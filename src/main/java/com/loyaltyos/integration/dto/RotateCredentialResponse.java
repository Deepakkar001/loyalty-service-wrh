package com.loyaltyos.integration.dto;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;

import java.time.Instant;

public class RotateCredentialResponse {

    private String keyId;
    private String apiKey;
    private String signingSecret;
    private ApiKeyEnvironment environment;
    private String keyPrefix;
    private Instant createdAt;
    private String revokedKeyId;
    private String message;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getSigningSecret() { return signingSecret; }
    public void setSigningSecret(String signingSecret) { this.signingSecret = signingSecret; }
    public ApiKeyEnvironment getEnvironment() { return environment; }
    public void setEnvironment(ApiKeyEnvironment environment) { this.environment = environment; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getRevokedKeyId() { return revokedKeyId; }
    public void setRevokedKeyId(String revokedKeyId) { this.revokedKeyId = revokedKeyId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
