package com.loyaltyos.onboarding.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /**
     * HS256 shared secret. Provide via env var in local/dev.
     * Minimum 32+ chars recommended.
     */
    private String secret;

    private String issuer = "loyaltyos-tenant-onboarding";

    /** Access token TTL in minutes. */
    private long accessTtlMinutes = 60;

    /** Refresh token TTL in days (stored server-side; sent as HttpOnly cookie). */
    private long refreshTtlDays = 14;

    /**
     * Cookie settings for refresh token.
     * In production, set secureCookies=true and configure cookieDomain if needed.
     */
    private boolean secureCookies = false;
    private String cookieDomain;
    private String refreshCookieName = "loyaltyos_rt";
    private String refreshCookieSameSite = "Strict";
}

