package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;

    private String tenantId;
    private String email;
    private OnboardingStatus onboardingStatus;
    private AgreementStatus latestAgreementStatus;
}

