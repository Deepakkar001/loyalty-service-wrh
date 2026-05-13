package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.AdminRole;

public class AdminLoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private String adminUid;
    private String email;
    private String fullName;
    private AdminRole role;

    public AdminLoginResponse() {}

    public AdminLoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String adminUid,
        String email,
        String fullName,
        AdminRole role
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.adminUid = adminUid;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String accessToken;
        private String tokenType;
        private long expiresInSeconds;
        private String adminUid;
        private String email;
        private String fullName;
        private AdminRole role;

        private Builder() {}

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder expiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; return this; }
        public Builder adminUid(String adminUid) { this.adminUid = adminUid; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(AdminRole role) { this.role = role; return this; }

        public AdminLoginResponse build() {
            return new AdminLoginResponse(accessToken, tokenType, expiresInSeconds, adminUid, email, fullName, role);
        }
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public String getAdminUid() { return adminUid; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public AdminRole getRole() { return role; }
}
