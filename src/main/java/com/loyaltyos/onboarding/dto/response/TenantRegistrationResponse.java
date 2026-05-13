package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;

import java.time.Instant;

public class TenantRegistrationResponse {
    private String tenantId;
    private String slug;
    private String email;
    private OnboardingStatus onboardingStatus;
    private IdentityMode identityMode;
    private String message;
    private Instant createdAt;

    public TenantRegistrationResponse() {}

    public TenantRegistrationResponse(
        String tenantId,
        String slug,
        String email,
        OnboardingStatus onboardingStatus,
        IdentityMode identityMode,
        String message,
        Instant createdAt
    ) {
        this.tenantId = tenantId;
        this.slug = slug;
        this.email = email;
        this.onboardingStatus = onboardingStatus;
        this.identityMode = identityMode;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String slug;
        private String email;
        private OnboardingStatus onboardingStatus;
        private IdentityMode identityMode;
        private String message;
        private Instant createdAt;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder onboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; return this; }
        public Builder identityMode(IdentityMode identityMode) { this.identityMode = identityMode; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TenantRegistrationResponse build() {
            return new TenantRegistrationResponse(tenantId, slug, email, onboardingStatus, identityMode, message, createdAt);
        }
    }

    public String getTenantId() { return tenantId; }
    public String getSlug() { return slug; }
    public String getEmail() { return email; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public IdentityMode getIdentityMode() { return identityMode; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}

