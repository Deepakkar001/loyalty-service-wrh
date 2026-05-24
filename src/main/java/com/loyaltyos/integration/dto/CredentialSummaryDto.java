package com.loyaltyos.integration.dto;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;

import java.time.Instant;

public class CredentialSummaryDto {

    private String keyId;
    private String keyPrefix;
    private String secretMasked;
    private ApiKeyEnvironment environment;
    private ApiKeyStatus status;
    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant lastSecretRevealedAt;
    private int requestCountLast24h;
    private String name;
    private String description;
    private boolean secretRetrievable;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getSecretMasked() { return secretMasked; }
    public void setSecretMasked(String secretMasked) { this.secretMasked = secretMasked; }
    public ApiKeyEnvironment getEnvironment() { return environment; }
    public void setEnvironment(ApiKeyEnvironment environment) { this.environment = environment; }
    public ApiKeyStatus getStatus() { return status; }
    public void setStatus(ApiKeyStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public Instant getLastSecretRevealedAt() { return lastSecretRevealedAt; }
    public void setLastSecretRevealedAt(Instant lastSecretRevealedAt) { this.lastSecretRevealedAt = lastSecretRevealedAt; }
    public int getRequestCountLast24h() { return requestCountLast24h; }
    public void setRequestCountLast24h(int requestCountLast24h) { this.requestCountLast24h = requestCountLast24h; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isSecretRetrievable() { return secretRetrievable; }
    public void setSecretRetrievable(boolean secretRetrievable) { this.secretRetrievable = secretRetrievable; }
}
