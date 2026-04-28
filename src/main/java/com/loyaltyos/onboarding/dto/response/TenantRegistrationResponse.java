package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TenantRegistrationResponse {
    private String tenantId;
    private String slug;
    private String email;
    private OnboardingStatus onboardingStatus;
    private IdentityMode identityMode;
    private String message;
    private Instant createdAt;
}

