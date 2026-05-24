package com.loyaltyos.integration.security;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String tenantId;
    private final String keyUid;
    private final ApiKeyEnvironment environment;

    public ApiKeyAuthentication(String tenantId, String keyUid, ApiKeyEnvironment environment) {
        super(List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
        this.tenantId = tenantId;
        this.keyUid = keyUid;
        this.environment = environment;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return keyUid;
    }

    /**
     * Dedicated principal object. Do not return {@code this} — Spring Security's
     * {@link org.springframework.security.authentication.AbstractAuthenticationToken#getName()}
     * calls {@code getPrincipal().getName()}, which recurses when principal is another
     * {@code AbstractAuthenticationToken}.
     */
    @Override
    public Object getPrincipal() {
        return new ApiKeyPrincipal(tenantId, keyUid, environment);
    }

    @Override
    public String getName() {
        return tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getKeyUid() {
        return keyUid;
    }

    public ApiKeyEnvironment getEnvironment() {
        return environment;
    }
}
