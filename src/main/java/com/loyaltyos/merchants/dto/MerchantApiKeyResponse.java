package com.loyaltyos.merchants.dto;

import java.time.Instant;

public class MerchantApiKeyResponse {

    private String keyUid;
    private String keyPrefix;
    private String name;
    private String environment;
    private boolean active;
    private Instant createdAt;
    private String apiKey;

    public String getKeyUid() { return keyUid; }
    public void setKeyUid(String keyUid) { this.keyUid = keyUid; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
