package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;

public class ApiKeyGeneratedResponse {
    private String keyUid;
    private String apiKey;
    private String signingSecret;
    private ApiKeyEnvironment environment;
    private String keyPrefix;
    private String message;

    public ApiKeyGeneratedResponse() {}

    public ApiKeyGeneratedResponse(
        String keyUid,
        String apiKey,
        String signingSecret,
        ApiKeyEnvironment environment,
        String keyPrefix,
        String message
    ) {
        this.keyUid = keyUid;
        this.apiKey = apiKey;
        this.signingSecret = signingSecret;
        this.environment = environment;
        this.keyPrefix = keyPrefix;
        this.message = message;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String keyUid;
        private String apiKey;
        private String signingSecret;
        private ApiKeyEnvironment environment;
        private String keyPrefix;
        private String message;

        private Builder() {}

        public Builder keyUid(String keyUid) { this.keyUid = keyUid; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder signingSecret(String signingSecret) { this.signingSecret = signingSecret; return this; }
        public Builder environment(ApiKeyEnvironment environment) { this.environment = environment; return this; }
        public Builder keyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public ApiKeyGeneratedResponse build() {
            return new ApiKeyGeneratedResponse(keyUid, apiKey, signingSecret, environment, keyPrefix, message);
        }
    }

    public String getKeyUid() { return keyUid; }
    public String getApiKey() { return apiKey; }
    public String getSigningSecret() { return signingSecret; }
    public ApiKeyEnvironment getEnvironment() { return environment; }
    public String getKeyPrefix() { return keyPrefix; }
    public String getMessage() { return message; }
}

