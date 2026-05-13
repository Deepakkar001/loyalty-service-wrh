package com.loyaltyos.onboarding.dto.response;

import java.time.Instant;

public class GoLiveActivateResponse {
    private String tenantId;
    private Instant activatedAt;
    private String message;

    public GoLiveActivateResponse() {}

    public GoLiveActivateResponse(String tenantId, Instant activatedAt, String message) {
        this.tenantId = tenantId;
        this.activatedAt = activatedAt;
        this.message = message;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private Instant activatedAt;
        private String message;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public GoLiveActivateResponse build() {
            return new GoLiveActivateResponse(tenantId, activatedAt, message);
        }
    }

    public String getTenantId() { return tenantId; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getMessage() { return message; }
}

