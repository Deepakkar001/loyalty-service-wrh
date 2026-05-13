package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;

import java.time.Instant;

public class ApiKeySummaryResponse {
    private ApiKeyEnvironment environment;
    private String keyUid;
    private String keyPrefix;
    private Instant createdAt;
    private Instant lastUsedAt;

    public ApiKeySummaryResponse() {}

    public ApiKeySummaryResponse(
        ApiKeyEnvironment environment,
        String keyUid,
        String keyPrefix,
        Instant createdAt,
        Instant lastUsedAt
    ) {
        this.environment = environment;
        this.keyUid = keyUid;
        this.keyPrefix = keyPrefix;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ApiKeyEnvironment environment;
        private String keyUid;
        private String keyPrefix;
        private Instant createdAt;
        private Instant lastUsedAt;

        private Builder() {}

        public Builder environment(ApiKeyEnvironment environment) { this.environment = environment; return this; }
        public Builder keyUid(String keyUid) { this.keyUid = keyUid; return this; }
        public Builder keyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder lastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; return this; }

        public ApiKeySummaryResponse build() {
            return new ApiKeySummaryResponse(environment, keyUid, keyPrefix, createdAt, lastUsedAt);
        }
    }

    public ApiKeyEnvironment getEnvironment() { return environment; }
    public String getKeyUid() { return keyUid; }
    public String getKeyPrefix() { return keyPrefix; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}

