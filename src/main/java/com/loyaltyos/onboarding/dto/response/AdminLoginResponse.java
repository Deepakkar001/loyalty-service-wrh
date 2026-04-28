package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.AdminRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private String adminUid;
    private String email;
    private String fullName;
    private AdminRole role;
}
