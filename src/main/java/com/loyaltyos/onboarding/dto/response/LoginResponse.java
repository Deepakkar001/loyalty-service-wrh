package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;

    private String tenantId;
    private String email;
    private OnboardingStatus onboardingStatus;
    private AgreementStatus latestAgreementStatus;

    public LoginResponse() {}

    public LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String tenantId,
        String email,
        OnboardingStatus onboardingStatus,
        AgreementStatus latestAgreementStatus
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.tenantId = tenantId;
        this.email = email;
        this.onboardingStatus = onboardingStatus;
        this.latestAgreementStatus = latestAgreementStatus;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String accessToken;
        private String tokenType;
        private long expiresInSeconds;
        private String tenantId;
        private String email;
        private OnboardingStatus onboardingStatus;
        private AgreementStatus latestAgreementStatus;

        private Builder() {}

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder expiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder onboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; return this; }
        public Builder latestAgreementStatus(AgreementStatus latestAgreementStatus) { this.latestAgreementStatus = latestAgreementStatus; return this; }

        public LoginResponse build() {
            return new LoginResponse(
                accessToken,
                tokenType,
                expiresInSeconds,
                tenantId,
                email,
                onboardingStatus,
                latestAgreementStatus
            );
        }
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public String getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public AgreementStatus getLatestAgreementStatus() { return latestAgreementStatus; }
}

