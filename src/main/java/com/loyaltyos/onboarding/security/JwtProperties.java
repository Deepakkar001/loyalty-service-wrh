package com.loyaltyos.onboarding.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

    public JwtProperties() {
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(long accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(long refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }

    public boolean isSecureCookies() {
        return secureCookies;
    }

    public void setSecureCookies(boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public void setRefreshCookieName(String refreshCookieName) {
        this.refreshCookieName = refreshCookieName;
    }

    public String getRefreshCookieSameSite() {
        return refreshCookieSameSite;
    }

    public void setRefreshCookieSameSite(String refreshCookieSameSite) {
        this.refreshCookieSameSite = refreshCookieSameSite;
    }
}

