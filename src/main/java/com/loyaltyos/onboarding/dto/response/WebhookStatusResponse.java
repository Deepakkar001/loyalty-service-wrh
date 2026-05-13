package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.WebhookVerificationStatus;

import java.time.Instant;

public class WebhookStatusResponse {
    private String endpointUrl;
    private WebhookVerificationStatus verificationStatus;
    private Instant lastVerifiedAt;

    public WebhookStatusResponse() {}

    public WebhookStatusResponse(String endpointUrl, WebhookVerificationStatus verificationStatus, Instant lastVerifiedAt) {
        this.endpointUrl = endpointUrl;
        this.verificationStatus = verificationStatus;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String endpointUrl;
        private WebhookVerificationStatus verificationStatus;
        private Instant lastVerifiedAt;

        private Builder() {}

        public Builder endpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; return this; }
        public Builder verificationStatus(WebhookVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public Builder lastVerifiedAt(Instant lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; return this; }

        public WebhookStatusResponse build() {
            return new WebhookStatusResponse(endpointUrl, verificationStatus, lastVerifiedAt);
        }
    }

    public String getEndpointUrl() { return endpointUrl; }
    public WebhookVerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
}

